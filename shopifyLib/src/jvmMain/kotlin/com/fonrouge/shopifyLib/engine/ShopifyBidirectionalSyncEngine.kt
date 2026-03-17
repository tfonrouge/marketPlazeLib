package com.fonrouge.shopifyLib.engine

import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.sync.IErpProductReader
import com.fonrouge.shopifyLib.IShopifyPushService
import com.fonrouge.shopifyLib.model.SyncDiff
import com.fonrouge.shopifyLib.model.SyncError
import com.fonrouge.shopifyLib.model.SyncReport

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
                listingSaver(
                    MarketplaceListing(
                        productId = product._id,
                        marketplaceId = marketplace._id,
                        externalId = shopifyId,
                        title = product.name,
                        price = product.priceLists.firstOrNull()?.price ?: product.baseCost,
                        publishEnabled = true,
                    )
                )
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

    private fun detectChanges(product: Product, listing: MarketplaceListing): List<String> {
        val changes = mutableListOf<String>()
        if (product.name != listing.title) changes += "title"
        val erpPrice = product.priceLists.firstOrNull()?.price ?: product.baseCost
        if (erpPrice != listing.price) changes += "price"
        return changes
    }
}
