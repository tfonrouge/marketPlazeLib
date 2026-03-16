package com.fonrouge.commonLib.service

import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*

object ProfitCalculator {
    fun calculate(
        salePrice: Money,
        baseCost: Money,
        costStructure: CostStructure,
        marketplace: Marketplace = Marketplace(name = "", type = ""),
        discount: Double = 0.0,
        presentation: ProductPresentation? = null,
        productId: ProductId = OId(),
        listingId: MarketplaceListingId? = null,
        shippingCostOverride: Money? = null,
    ): CostBreakdown {
        val finalPrice = if (discount > 0.0) {
            Money((salePrice.cents * (1.0 - discount)).toLong())
        } else salePrice

        val taxableBase = if (marketplace.ivaIncluded && finalPrice.cents > 0) {
            Money((finalPrice.cents / 1.16).toLong())
        } else finalPrice

        val ivaWithheld = if (marketplace.ivaRate > 0.0) {
            Money((taxableBase.cents * marketplace.ivaRate).toLong())
        } else Money.ZERO

        val isrWithheld = if (marketplace.isrRate > 0.0) {
            Money((taxableBase.cents * marketplace.isrRate).toLong())
        } else Money.ZERO

        val lineItems = mutableListOf<FeeLineItem>()

        for (commission in costStructure.commissions) {
            val amount = Money((finalPrice.cents * commission.percentage / 100.0).toLong()) + commission.fixedAmount
            lineItems += FeeLineItem(name = commission.name, amount = amount, type = "commission")
        }

        for (fee in costStructure.fees) {
            lineItems += FeeLineItem(name = fee.name, amount = fee.amount, type = "fee")
        }

        for (modality in costStructure.publicationModalities) {
            if (modality.fee != Money.ZERO) {
                lineItems += FeeLineItem(name = modality.name, amount = modality.fee, type = "modality")
            }
        }

        if (ivaWithheld != Money.ZERO) {
            lineItems += FeeLineItem(name = "IVA Retenido", amount = ivaWithheld, type = "iva_withholding")
        }
        if (isrWithheld != Money.ZERO) {
            lineItems += FeeLineItem(name = "ISR Retenido", amount = isrWithheld, type = "isr_withholding")
        }

        val shippingCost = shippingCostOverride
            ?: resolveShippingCost(finalPrice, costStructure.shippingTiers)

        if (shippingCost != Money.ZERO) {
            lineItems += FeeLineItem(name = "Envío", amount = shippingCost, type = "shipping")
        }

        val packagingCost = presentation?.packagingCost ?: Money.ZERO

        val totalFees = lineItems.fold(Money.ZERO) { acc, item -> acc + item.amount }
        val totalCosts = baseCost + packagingCost + totalFees
        val netProfit = finalPrice - totalCosts
        val profitMarginPercent = if (finalPrice.cents > 0) {
            netProfit.cents.toDouble() / finalPrice.cents.toDouble() * 100.0
        } else 0.0

        val healthLevel = HealthLevel.classify(profitMarginPercent)
        val piecesCount = presentation?.totalPieces?.takeIf { it > 0 }

        return CostBreakdown(
            productId = productId,
            marketplaceId = costStructure.marketplaceId,
            listingId = listingId,
            salePrice = salePrice,
            finalPrice = finalPrice,
            baseCost = baseCost,
            taxableBase = taxableBase,
            ivaWithheld = ivaWithheld,
            isrWithheld = isrWithheld,
            shippingCost = shippingCost,
            lineItems = lineItems,
            totalCosts = totalCosts,
            netProfit = netProfit,
            profitMarginPercent = profitMarginPercent,
            healthLevel = healthLevel.name,
            piecesCount = piecesCount,
            pricePerPiece = piecesCount?.let { Money(finalPrice.cents / it) },
            costPerPiece = piecesCount?.let { Money(totalCosts.cents / it) },
            profitPerPiece = piecesCount?.let { Money(netProfit.cents / it) },
        )
    }
}

fun resolveShippingCost(price: Money, tiers: List<ShippingTier>): Money {
    if (tiers.isEmpty()) return Money.ZERO
    val priceValue = price.toDecimal()
    val priceTiers = tiers.filter { it.tierType == "price" }.sortedBy { it.minValue }
    for (tier in priceTiers) {
        if (priceValue >= tier.minValue && priceValue < tier.maxValue) {
            return tier.cost
        }
    }
    return Money.ZERO
}
