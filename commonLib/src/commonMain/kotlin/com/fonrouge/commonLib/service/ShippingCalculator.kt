package com.fonrouge.commonLib.service

import com.fonrouge.commonLib.model.*
import kotlin.math.max

object ShippingCalculator {

    fun calculateShippingCost(
        productWeight: Double,
        presentation: ProductPresentation?,
        salePrice: Money,
        envioTiers: List<EnvioTier>,
        envioDiscounts: List<EnvioDiscount>,
        envioOverride: Money? = null,
    ): Money {
        if (envioOverride != null) return envioOverride

        val weight = presentation?.packageWeight?.takeIf { it > 0 } ?: productWeight
        if (weight <= 0 || envioTiers.isEmpty()) return Money.ZERO

        val volumetricWeight = if (presentation != null &&
            presentation.packageLength > 0 && presentation.packageWidth > 0 && presentation.packageHeight > 0
        ) {
            (presentation.packageLength * presentation.packageWidth * presentation.packageHeight) / 5000.0
        } else 0.0

        val billableWeight = max(weight, volumetricWeight)
        val baseCost = resolveEnvioTier(billableWeight, envioTiers)
        if (baseCost == Money.ZERO) return Money.ZERO

        val discountRate = resolveEnvioDiscount(salePrice, envioDiscounts)
        return Money((baseCost.cents * (1.0 - discountRate)).toLong())
    }

    fun resolveCargoFijo(
        finalPrice: Money,
        cargoFijoTiers: List<CargoFijoTier>,
        cargoFijoOverride: Money? = null,
    ): Money {
        if (cargoFijoOverride != null) return cargoFijoOverride
        if (cargoFijoTiers.isEmpty()) return Money.ZERO

        val priceValue = finalPrice.toDecimal()
        val sorted = cargoFijoTiers.sortedBy { it.minPrice }
        for (tier in sorted) {
            val max = tier.maxPrice ?: Double.MAX_VALUE
            if (priceValue >= tier.minPrice && priceValue < max) {
                return tier.cost
            }
        }
        return Money.ZERO
    }

    private fun resolveEnvioTier(weight: Double, tiers: List<EnvioTier>): Money {
        val sorted = tiers.sortedBy { it.minWeight }
        for (tier in sorted) {
            val max = tier.maxWeight ?: Double.MAX_VALUE
            if (weight >= tier.minWeight && weight < max) {
                return tier.cost
            }
        }
        return Money.ZERO
    }

    private fun resolveEnvioDiscount(salePrice: Money, discounts: List<EnvioDiscount>): Double {
        if (discounts.isEmpty()) return 0.0
        val priceValue = salePrice.toDecimal()
        val sorted = discounts.sortedBy { it.minPrice }
        for (discount in sorted) {
            val max = discount.maxPrice ?: Double.MAX_VALUE
            if (priceValue >= discount.minPrice && priceValue < max) {
                return discount.discountRate
            }
        }
        return 0.0
    }
}
