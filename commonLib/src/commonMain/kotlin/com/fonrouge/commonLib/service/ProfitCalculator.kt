package com.fonrouge.commonLib.service

import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*

object ProfitCalculator {
    fun calculate(
        salePrice: Money,
        baseCost: Money,
        costStructure: CostStructure,
        productId: ProductId = OId(),
        listingId: MarketplaceListingId? = null,
    ): CostBreakdown {
        val lineItems = mutableListOf<FeeLineItem>()

        for (commission in costStructure.commissions) {
            val amount = Money((salePrice.cents * commission.percentage / 100.0).toLong()) + commission.fixedAmount
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

        val totalFees = lineItems.fold(Money.ZERO) { acc, item -> acc + item.amount }
        val totalCosts = baseCost + totalFees
        val netProfit = salePrice - totalCosts
        val profitMarginPercent = if (salePrice.cents > 0) {
            netProfit.cents.toDouble() / salePrice.cents.toDouble() * 100.0
        } else 0.0

        return CostBreakdown(
            productId = productId,
            marketplaceId = costStructure.marketplaceId,
            listingId = listingId,
            salePrice = salePrice,
            baseCost = baseCost,
            lineItems = lineItems,
            totalCosts = totalCosts,
            netProfit = netProfit,
            profitMarginPercent = profitMarginPercent,
        )
    }
}
