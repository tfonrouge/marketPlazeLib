package com.fonrouge.commonLib.model

import com.fonrouge.base.api.IApiFilter
import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceFilter(
    override var masterItemId: Unit? = null,
    var type: String? = null,
    var enabled: Boolean? = null,
) : IApiFilter<Unit>()

@Serializable
data class ProductFilter(
    override var masterItemId: Unit? = null,
    var sku: String? = null,
    var name: String? = null,
) : IApiFilter<Unit>()

@Serializable
data class MarketplaceListingFilter(
    override var masterItemId: MarketplaceId? = null,
    var productId: ProductId? = null,
    var publishEnabled: Boolean? = null,
) : IApiFilter<MarketplaceId>()

@Serializable
data class CostStructureFilter(
    override var masterItemId: MarketplaceId? = null,
) : IApiFilter<MarketplaceId>()

@Serializable
data class CostBreakdownFilter(
    override var masterItemId: MarketplaceId? = null,
    var productId: ProductId? = null,
) : IApiFilter<MarketplaceId>()

@Serializable
data class CompetitorPriceFilter(
    override var masterItemId: MarketplaceId? = null,
    var productId: ProductId? = null,
    var competitorName: String? = null,
) : IApiFilter<MarketplaceId>()
