package com.fonrouge.shopifyLib.model

import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductId
import kotlinx.serialization.Serializable

@Serializable
data class SyncDiff(
    val product: Product,
    val shopifyId: StringId<ShopifyProduct>,
    val changedFields: List<String> = emptyList(),
)

@Serializable
data class SyncError(
    val productId: ProductId? = null,
    val message: String,
)

@Serializable
data class SyncReport(
    val toCreate: List<Product> = emptyList(),
    val toUpdate: List<SyncDiff> = emptyList(),
    val orphaned: List<ShopifyProduct> = emptyList(),
    val inSync: Int = 0,
    val errors: List<SyncError> = emptyList(),
)
