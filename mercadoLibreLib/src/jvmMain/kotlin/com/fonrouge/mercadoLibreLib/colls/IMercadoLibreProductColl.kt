package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.bson.toBsonSet
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.eq

abstract class IMercadoLibreProductColl<CC : CommonMercadoLibreProduct<FILT>, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, MercadoLibreProduct, StringId<MercadoLibreProduct>, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
), IMarketplaceSync<MercadoLibreProduct> {

    abstract suspend fun fetchProducts(marketplace: Marketplace): List<MercadoLibreProduct>

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
