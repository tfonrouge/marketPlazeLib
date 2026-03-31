package com.fonrouge.shopifyLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

typealias ShopifyCollectionId = StringId<ShopifyCollection>

@Serializable
data class ShopifyCollection(
    override val _id: ShopifyCollectionId,
    val marketplaceId: MarketplaceId = OId(),
    val title: String,
    val handle: String,
    val descriptionHtml: String? = null,
    val sortOrder: String = "BEST_SELLING",
    val productsCount: Int = 0,
    val rules: List<CollectionRule> = emptyList(),
    val isSmartCollection: Boolean = false,
    val image: String? = null,
) : BaseDoc<ShopifyCollectionId>
