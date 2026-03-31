package com.fonrouge.shopifyLib.engine

import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.sync.IErpProductReader
import com.fonrouge.shopifyLib.IShopifyPushService
import com.fonrouge.shopifyLib.model.BulkSyncResult
import com.fonrouge.shopifyLib.model.FieldChange
import com.fonrouge.shopifyLib.model.SyncDiff
import com.fonrouge.shopifyLib.model.SyncError
import com.fonrouge.shopifyLib.model.SyncReport
import kotlinx.coroutines.delay

class ShopifyBidirectionalSyncEngine(
    private val erpReader: IErpProductReader,
    private val pushService: IShopifyPushService,
    private val listingsProvider: suspend (Marketplace) -> List<MarketplaceListing>,
    private val listingSaver: suspend (MarketplaceListing) -> Unit,
) {

    suspend fun reconcile(marketplace: Marketplace): SyncReport {
        val erpProducts = try {
            erpReader.readProducts()
        } catch (e: Exception) {
            return SyncReport(errors = listOf(SyncError(message = "ERP read failed: ${e.message}")))
        }

        val listings = listingsProvider(marketplace)
        val listingsByProductId = listings.associateBy { it.productId }

        val toCreate = mutableListOf<Product>()
        val toUpdate = mutableListOf<SyncDiff>()
        var inSync = 0

        for (product in erpProducts) {
            if (!product.publishEnabled) continue

            val listing = listingsByProductId[product._id]
            if (listing == null) {
                toCreate += product
            } else {
                val changedFields = detectChanges(product, listing)
                if (changedFields.isNotEmpty()) {
                    toUpdate += SyncDiff(
                        product = product,
                        shopifyId = StringId(listing.externalId),
                        changedFields = changedFields,
                    )
                } else {
                    inSync++
                }
            }
        }

        return SyncReport(
            toCreate = toCreate,
            toUpdate = toUpdate,
            inSync = inSync,
        )
    }

    suspend fun syncToRemote(marketplace: Marketplace): SimpleState {
        val report = reconcile(marketplace)
        if (report.errors.isNotEmpty()) {
            return SimpleState(isOk = false, msgError = "Reconcile failed: ${report.errors.first().message}")
        }

        var created = 0
        var updated = 0
        val errors = mutableListOf<String>()

        for (product in report.toCreate) {
            val result = pushService.createProduct(marketplace, product)
            if (!result.hasError) {
                val shopifyId = result.msgOk ?: ""
                listingSaver(product.toListing(marketplace._id, shopifyId))
                created++
            } else {
                errors += "Create ${product.sku}: ${result.msgError}"
            }
        }

        for (diff in report.toUpdate) {
            val result = pushService.updateProduct(marketplace, diff.product, diff.shopifyId.id)
            if (!result.hasError) {
                updated++
            } else {
                errors += "Update ${diff.product.sku}: ${result.msgError}"
            }
        }

        val summary = "Created: $created, Updated: $updated, InSync: ${report.inSync}" +
            if (errors.isNotEmpty()) ", Errors: ${errors.size}" else ""

        return SimpleState(
            isOk = errors.isEmpty(),
            msgOk = if (errors.isEmpty()) summary else null,
            msgError = if (errors.isNotEmpty()) "$summary — ${errors.joinToString("; ")}" else null,
        )
    }

    private fun detectChanges(product: Product, listing: MarketplaceListing): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()
        fun check(field: String, newValue: String?, oldValue: String?) {
            if (newValue != oldValue) changes += FieldChange(field, oldValue, newValue)
        }
        check("title", product.name, listing.title)
        val erpPrice = product.priceLists.firstOrNull()?.price ?: product.baseCost
        check("price", "%.2f".format(erpPrice.toDecimal()), "%.2f".format(listing.price.toDecimal()))
        check("descriptionHtml", product.descriptionHtml, listing.descriptionHtml)
        check("barcode", product.barcode, listing.barcode)
        check("vendor", product.vendor, listing.vendor)
        check("weight", product.weight?.let { "%.3f".format(it) }, listing.weight?.let { "%.3f".format(it) })
        check("tags", product.tags.sorted().joinToString(","), listing.tags.sorted().joinToString(","))
        check("published", product.publishEnabled.toString(), listing.publishEnabled.toString())
        check("inventoryQuantity", product.inventoryQuantity.toString(), listing.inventoryQuantity.toString())
        check("productType", product.productType, listing.productType)
        return changes
    }

    private fun Product.toListing(marketplaceId: MarketplaceId, externalId: String) = MarketplaceListing(
        productId = _id,
        marketplaceId = marketplaceId,
        externalId = externalId,
        title = name,
        price = priceLists.firstOrNull()?.price ?: baseCost,
        publishEnabled = publishEnabled,
        descriptionHtml = descriptionHtml,
        barcode = barcode,
        vendor = vendor,
        weight = weight,
        tags = tags,
        inventoryQuantity = inventoryQuantity,
        productType = productType,
    )

    suspend fun bulkSyncToRemote(
        marketplace: Marketplace,
        batchSize: Int = 50,
        delayMs: Long = 500,
    ): BulkSyncResult {
        val report = reconcile(marketplace)
        if (report.errors.isNotEmpty()) {
            return BulkSyncResult(errors = report.errors)
        }

        var created = 0
        var updated = 0
        var failed = 0
        val errors = mutableListOf<SyncError>()

        for (batch in report.toCreate.chunked(batchSize)) {
            for (product in batch) {
                val result = pushService.createProduct(marketplace, product)
                if (!result.hasError) {
                    val shopifyId = result.msgOk ?: ""
                    listingSaver(product.toListing(marketplace._id, shopifyId))
                    created++
                } else {
                    failed++
                    errors += SyncError(productId = product._id, message = "Create: ${result.msgError}")
                }
                delay(delayMs)
            }
        }

        for (batch in report.toUpdate.chunked(batchSize)) {
            for (diff in batch) {
                val result = pushService.updateProduct(marketplace, diff.product, diff.shopifyId.id)
                if (!result.hasError) {
                    listingSaver(diff.product.toListing(marketplace._id, diff.shopifyId.id))
                    updated++
                } else {
                    failed++
                    errors += SyncError(productId = diff.product._id, message = "Update: ${result.msgError}")
                }
                delay(delayMs)
            }
        }

        return BulkSyncResult(created = created, updated = updated, failed = failed, errors = errors)
    }
}
