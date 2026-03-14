package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias CostStructureId = OId<CostStructure>

@Serializable
data class CostStructure(
    override val _id: CostStructureId = OId(),
    val marketplaceId: MarketplaceId,
    val name: String,
    val commissions: List<Commission> = emptyList(),
    val fees: List<Fee> = emptyList(),
    val shippingTiers: List<ShippingTier> = emptyList(),
    val advertisingRates: List<AdvertisingRate> = emptyList(),
    val publicationModalities: List<PublicationModality> = emptyList(),
) : BaseDoc<CostStructureId>
