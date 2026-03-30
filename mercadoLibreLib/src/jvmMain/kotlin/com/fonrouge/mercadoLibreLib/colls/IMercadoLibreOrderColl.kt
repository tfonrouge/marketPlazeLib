package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.bson.toBsonSet
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreOrder
import com.fonrouge.mercadoLibreLib.model.MercadoLibreOrder
import com.fonrouge.mercadoLibreLib.model.MercadoLibreOrderFilter
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.eq

abstract class IMercadoLibreOrderColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreOrder, StringId<MercadoLibreOrder>, MercadoLibreOrderFilter, UID>(
    commonContainer = CommonMercadoLibreOrder,
    debug = debug,
), IMarketplaceSync<MercadoLibreOrder> {

    abstract suspend fun fetchOrders(marketplace: Marketplace): List<MercadoLibreOrder>

    override suspend fun syncToRemote(marketplace: Marketplace): SimpleState {
        return SimpleState(isOk = false, "syncToRemote not supported for MercadoLibre orders")
    }

    override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
        val initTime = System.currentTimeMillis()
        val orders = try {
            fetchOrders(marketplace)
        } catch (e: Exception) {
            return simpleErrorState("MercadoLibre fetch error: ${e.message}")
        }
        if (orders.isEmpty()) {
            val useTime = System.currentTimeMillis() - initTime
            return SimpleState(isOk = true, "No orders found, $useTime ms used")
        }
        val updateOneModels = orders.map { order ->
            UpdateOneModel<MercadoLibreOrder>(
                BaseDoc<StringId<MercadoLibreOrder>>::_id eq order._id,
                order.toBsonSet(),
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
