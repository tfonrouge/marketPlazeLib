package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.PriceListEntry
import com.fonrouge.commonLib.model.Product

interface IErpProductReader {
    suspend fun readProducts(): List<Product>
    suspend fun readProductById(erpId: String): Product?
    suspend fun readInventoryLevels(): Map<String, Int>        // sku → quantity
    suspend fun readPriceLists(): Map<String, List<PriceListEntry>>  // sku → prices
    suspend fun readInventoryByLocation(): Map<String, Map<String, Int>> = emptyMap()  // sku → (locationId → qty)
}
