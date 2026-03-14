package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

typealias CompetitorPriceId = OId<CompetitorPrice>

@Serializable
data class CompetitorPrice(
    override val _id: CompetitorPriceId = OId(),
    val marketplaceId: MarketplaceId,
    val productId: ProductId? = null,
    val competitorName: String,
    val price: Money,
    val observedAt: Instant,
) : BaseDoc<CompetitorPriceId>
