package com.fonrouge.commonLib.model

import kotlinx.serialization.Serializable

@Serializable
data class Commission(
    val name: String,
    val percentage: Double,
    val fixedAmount: Money = Money.ZERO,
)

@Serializable
data class Fee(
    val name: String,
    val amount: Money,
    val type: String = "fixed",
)

@Serializable
data class ShippingTier(
    val name: String,
    val minValue: Double = 0.0,
    val maxValue: Double = Double.MAX_VALUE,
    val tierType: String = "price",
    val cost: Money,
)

@Serializable
data class AdvertisingRate(
    val name: String,
    val costPerClick: Money = Money.ZERO,
    val costPerImpression: Money = Money.ZERO,
)

@Serializable
data class PublicationModality(
    val name: String,
    val fee: Money = Money.ZERO,
    val features: List<String> = emptyList(),
)

@Serializable
data class FeeLineItem(
    val name: String,
    val amount: Money,
    val type: String,
)

@Serializable
data class ProductVariant(
    val sku: String = "",
    val name: String = "",
    val price: Money = Money.ZERO,
    val inventory: Int = 0,
    val compareAtPrice: Money? = null,
    val barcode: String? = null,
    val weight: Double? = null,
    val weightUnit: String? = null,
    val options: Map<String, String> = emptyMap(),
)

@Serializable
data class ProductImage(
    val url: String,
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val position: Int = 0,
)

@Serializable
data class ProductPresentation(
    val label: String,
    val piecesPerPackage: Int = 1,
    val packagesPerDisplay: Int = 1,
    val packagingCost: Money = Money.ZERO,
    val isHypothetical: Boolean = false,
    val packageWeight: Double = 0.0,
    val packageLength: Double = 0.0,
    val packageWidth: Double = 0.0,
    val packageHeight: Double = 0.0,
) {
    val totalPieces: Int get() = piecesPerPackage * packagesPerDisplay
}

@Serializable
data class PriceListEntry(
    val name: String,
    val price: Money,
    val currencyCode: String = "MXN",
    val compareAtPrice: Money? = null,
)

@Serializable
data class ProductOption(
    val name: String,
    val values: List<String> = emptyList(),
)

@Serializable
data class CargoFijoTier(
    val minPrice: Double,
    val maxPrice: Double? = null,
    val cost: Money,
)

@Serializable
data class EnvioTier(
    val minWeight: Double,
    val maxWeight: Double? = null,
    val cost: Money,
)

@Serializable
data class EnvioDiscount(
    val minPrice: Double,
    val maxPrice: Double? = null,
    val discountRate: Double,
)

@Serializable
enum class PublicationType {
    CLASICA,
    PREMIUM,
}

enum class HealthLevel(val label: String, val minMargin: Double) {
    NO_DATA("Sin datos", -1.0),
    CRITICAL("Crítico", 0.0),
    ACCEPTABLE("Aceptable", 10.0),
    HEALTHY("Saludable", 15.0),
    EXCELLENT("Excelente", 30.0);

    companion object {
        fun classify(marginPercent: Double, baseCost: Money = Money(1)): HealthLevel = when {
            baseCost.cents <= 0 -> NO_DATA
            marginPercent >= 30.0 -> EXCELLENT
            marginPercent >= 15.0 -> HEALTHY
            marginPercent >= 10.0 -> ACCEPTABLE
            else -> CRITICAL
        }
    }
}
