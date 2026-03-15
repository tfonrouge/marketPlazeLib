package com.fonrouge.amazonLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

@Serializable
data class AmazonProductFilter(
    override var masterItemId: MarketplaceId? = null,
) : IApiFilter<MarketplaceId>()
