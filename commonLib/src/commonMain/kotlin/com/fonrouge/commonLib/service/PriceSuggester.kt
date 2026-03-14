package com.fonrouge.commonLib.service

import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CostStructure
import com.fonrouge.commonLib.model.Money

object PriceSuggester {
    fun minimumPriceForMargin(
        baseCost: Money,
        costStructure: CostStructure,
        targetMarginPercent: Double,
    ): Money {
        val fixedFees = costStructure.fees.sumOf { it.amount.cents }
        val fixedCommissions = costStructure.commissions.sumOf { it.fixedAmount.cents }
        val modalityFees = costStructure.publicationModalities.sumOf { it.fee.cents }
        val fixedCosts = baseCost.cents + fixedFees + fixedCommissions + modalityFees

        val totalCommissionPercent = costStructure.commissions.sumOf { it.percentage }

        val denominator = 1.0 - (totalCommissionPercent / 100.0) - (targetMarginPercent / 100.0)
        if (denominator <= 0) return Money(Long.MAX_VALUE)

        val minPrice = (fixedCosts.toDouble() / denominator).toLong()
        return Money(minPrice)
    }

    fun competitivePrice(
        competitorPrices: List<CompetitorPrice>,
        strategy: PriceStrategy = PriceStrategy.MATCH_LOWEST,
    ): Money? {
        if (competitorPrices.isEmpty()) return null
        val prices = competitorPrices.map { it.price.cents }
        return when (strategy) {
            PriceStrategy.MATCH_LOWEST -> Money(prices.min())
            PriceStrategy.BELOW_AVERAGE -> Money((prices.average() * 0.95).toLong())
            PriceStrategy.MATCH_AVERAGE -> Money(prices.average().toLong())
        }
    }

    enum class PriceStrategy {
        MATCH_LOWEST,
        BELOW_AVERAGE,
        MATCH_AVERAGE,
    }
}
