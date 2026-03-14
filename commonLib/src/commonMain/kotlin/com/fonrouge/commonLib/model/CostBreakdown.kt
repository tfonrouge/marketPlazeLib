package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias CostBreakdownId = OId<CostBreakdown>

@Serializable
data class CostBreakdown(
    override val _id: CostBreakdownId = OId(),
    val productId: ProductId,
    val marketplaceId: MarketplaceId,
    val listingId: MarketplaceListingId? = null,
    val salePrice: Money,
    val baseCost: Money,
    val lineItems: List<FeeLineItem> = emptyList(),
    val totalCosts: Money,
    val netProfit: Money,
    val profitMarginPercent: Double,
) : BaseDoc<CostBreakdownId>
