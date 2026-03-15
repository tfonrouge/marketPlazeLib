package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

@Serializable
data class MercadoLibreProductFilter(
    override var masterItemId: MarketplaceId? = null,
) : IApiFilter<MarketplaceId>()
