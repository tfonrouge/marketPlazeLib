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
    val minWeight: Double = 0.0,
    val maxWeight: Double = Double.MAX_VALUE,
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
)

@Serializable
data class ProductImage(
    val url: String,
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)
