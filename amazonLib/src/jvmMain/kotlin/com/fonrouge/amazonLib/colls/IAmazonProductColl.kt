package com.fonrouge.amazonLib.colls

import com.fonrouge.amazonLib.common.CommonAmazonProduct
import com.fonrouge.amazonLib.model.AmazonProduct
import com.fonrouge.amazonLib.model.AmazonProductFilter
import com.fonrouge.base.bson.toBsonSet
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.fullStack.mongoDb.Coll
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.eq

abstract class IAmazonProductColl<UID : Any>(
    debug: Boolean = false,
) : Coll<AmazonProduct, StringId<AmazonProduct>, AmazonProductFilter, UID>(
    commonContainer = CommonAmazonProduct,
    debug = debug,
), IMarketplaceSync<AmazonProduct> {

    abstract suspend fun fetchProducts(marketplace: Marketplace): List<AmazonProduct>

    override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
        val initTime = System.currentTimeMillis()
        val products = try {
            fetchProducts(marketplace)
        } catch (e: Exception) {
            return simpleErrorState("Amazon fetch error: ${e.message}")
        }
        if (products.isEmpty()) {
            val useTime = System.currentTimeMillis() - initTime
            return SimpleState(isOk = true, "No products found, $useTime ms used")
        }
        val updateOneModels = products.map { product ->
            UpdateOneModel<AmazonProduct>(
                BaseDoc<StringId<AmazonProduct>>::_id eq product._id,
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
