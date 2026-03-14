package com.fonrouge.commonLib.service

import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*

object MarketplaceComparator {
    data class ComparisonResult(
        val breakdowns: List<CostBreakdown>,
        val bestByProfit: CostBreakdown?,
        val bestByMargin: CostBreakdown?,
    )

    fun compare(
        salePrice: Money,
        baseCost: Money,
        costStructures: List<CostStructure>,
        productId: ProductId = OId(),
    ): ComparisonResult {
        val breakdowns = costStructures.map { cs ->
            ProfitCalculator.calculate(
                salePrice = salePrice,
                baseCost = baseCost,
                costStructure = cs,
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
