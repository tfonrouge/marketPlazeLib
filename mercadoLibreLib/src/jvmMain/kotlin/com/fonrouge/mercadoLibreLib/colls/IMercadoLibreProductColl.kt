package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.bson.toBsonSet
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProductFilter
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.eq

abstract class IMercadoLibreProductColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreProduct, StringId<MercadoLibreProduct>, MercadoLibreProductFilter, UID>(
    commonContainer = CommonMercadoLibreProduct,
    debug = debug,
), IMarketplaceSync<MercadoLibreProduct> {

    abstract suspend fun fetchProducts(marketplace: Marketplace): List<MercadoLibreProduct>

    /**
     * Targeted multi-get of just [itemIds] (MarketPlace(MODULE) 11d / D71) — the freshness primitive
     * behind [refreshItems]. Returns real ML data (never a synthetic product), and unlike
     * [fetchProducts]'s `/users/{id}/items/search` it also returns items ML has already `closed`
     * (which the search endpoint drops), so a just-closed listing still refreshes correctly.
     */
    abstract suspend fun fetchProductsByIds(marketplace: Marketplace, itemIds: List<String>): List<MercadoLibreProduct>

    override suspend fun syncToRemote(marketplace: Marketplace): SimpleState {
        return SimpleState(isOk = false, "syncToRemote not supported for MercadoLibre")
    }

    override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
        val initTime = System.currentTimeMillis()
        val products = try {
            fetchProducts(marketplace)
        } catch (e: Exception) {
            return simpleErrorState("MercadoLibre fetch error: ${e.message}")
        }
        if (products.isEmpty()) {
            val useTime = System.currentTimeMillis() - initTime
            return SimpleState(isOk = true, "No products found, $useTime ms used")
        }
        return upsertProducts(products, initTime)
    }

    /**
     * Refresh just [itemIds] into the local mirror (11d / D71) — used right after a publish (to fold
     * the new listing in with real status at once) or a status change (pause/close/reactivate), so
     * the app reflects the current ML status without waiting for the periodic full sync.
     */
    suspend fun refreshItems(marketplace: Marketplace, itemIds: List<String>): SimpleState {
        if (itemIds.isEmpty()) return SimpleState(isOk = true, "No item ids to refresh")
        val initTime = System.currentTimeMillis()
        val products = try {
            fetchProductsByIds(marketplace, itemIds)
        } catch (e: Exception) {
            return simpleErrorState("MercadoLibre fetch-by-ids error: ${e.message}")
        }
        if (products.isEmpty()) {
            return SimpleState(isOk = true, "No products returned for ${itemIds.size} id(s)")
        }
        return upsertProducts(products, initTime)
    }

    private suspend fun upsertProducts(products: List<MercadoLibreProduct>, initTime: Long): SimpleState {
        val updateOneModels = products.map { product ->
            UpdateOneModel<MercadoLibreProduct>(
                BaseDoc<StringId<MercadoLibreProduct>>::_id eq product._id,
                product.toBsonSet(),
                UpdateOptions().upsert(true)
            )
        }
        val useTime = System.currentTimeMillis() - initTime
        val bulkWriteResult = coroutine.bulkWrite(updateOneModels)
        return if (bulkWriteResult.wasAcknowledged()) {
            SimpleState(isOk = true, "Updated ${bulkWriteResult.modifiedCount} documents, $useTime ms used")
        } else {
            SimpleState(isOk = false, "Bulk write operation was not acknowledged")
        }
    }
}
