package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias MarketplaceId = OId<Marketplace>

@Serializable
data class Marketplace(
    override val _id: MarketplaceId = OId(),
    val name: String,
    val type: String,
    val connectionConfig: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val ivaRate: Double = 0.0,
    val isrRate: Double = 0.0,
    val ivaIncluded: Boolean = true,
) : BaseDoc<MarketplaceId>
