package com.fonrouge.commonLib.service

import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*

object MarketplaceComparator {
    data class MarketplaceCostPair(
        val marketplace: Marketplace,
        val costStructure: CostStructure,
    )

    data class ComparisonResult(
        val breakdowns: List<CostBreakdown>,
        val bestByProfit: CostBreakdown?,
        val bestByMargin: CostBreakdown?,
    )

    fun compare(
        salePrice: Money,
        baseCost: Money,
        pairs: List<MarketplaceCostPair>,
        discount: Double = 0.0,
        presentation: ProductPresentation? = null,
        productId: ProductId = OId(),
    ): ComparisonResult {
        val breakdowns = pairs.map { (marketplace, costStructure) ->
            ProfitCalculator.calculate(
                salePrice = salePrice,
                baseCost = baseCost,
                costStructure = costStructure,
                marketplace = marketplace,
                discount = discount,
                presentation = presentation,
                productId = productId,
            )
        }
        return ComparisonResult(
            breakdowns = breakdowns,
            bestByProfit = breakdowns.maxByOrNull { it.netProfit.cents },
            bestByMargin = breakdowns.maxByOrNull { it.profitMarginPercent },
        )
    }
}
