package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

@Serializable
data class MercadoLibreOrderFilter(
    override var masterItemId: MarketplaceId? = null,
    var status: String? = null,
) : IApiFilter<MarketplaceId>()
