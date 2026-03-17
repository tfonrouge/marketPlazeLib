package com.fonrouge.shopifyLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

@Serializable
data class ShopifyCollectionFilter(
    override var masterItemId: MarketplaceId? = null,
) : IApiFilter<MarketplaceId>()
