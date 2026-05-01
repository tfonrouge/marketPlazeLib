package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias MarketplaceId = OId<Marketplace>

object MarketplaceType {
    const val MERCADO_LIBRE = "MercadoLibre"
    const val SHOPIFY = "Shopify"
    const val AMAZON = "Amazon"
}

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
    val commissionRateClassic: Double = 0.0,
    val commissionRatePremium: Double = 0.0,
    val cargoFijoTiers: List<CargoFijoTier> = emptyList(),
    val envioTiers: List<EnvioTier> = emptyList(),
    val envioDiscounts: List<EnvioDiscount> = emptyList(),
    val isDefault: Boolean = false,
    val minimumPrice: Money = Money.ZERO,
) : BaseDoc<MarketplaceId>
