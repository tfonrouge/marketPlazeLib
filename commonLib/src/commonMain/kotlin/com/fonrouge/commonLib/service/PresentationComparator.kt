package com.fonrouge.commonLib.service

import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*

object PresentationComparator {

    data class PresentationInput(
        val presentation: ProductPresentation,
        val salePrice: Money,
    )

    data class PresentationComparisonResult(
        val breakdowns: List<Pair<ProductPresentation, CostBreakdown>>,
        val bestByProfitPerPiece: ProductPresentation?,
    )

    fun compare(
        inputs: List<PresentationInput>,
        baseCost: Money,
        costStructure: CostStructure,
        marketplace: Marketplace = Marketplace(name = "", type = ""),
        productId: ProductId = OId(),
        publicationType: PublicationType = PublicationType.CLASICA,
        referralKitCost: Money = Money.ZERO,
        productWeight: Double = 0.0,
    ): PresentationComparisonResult {
        val breakdowns = inputs.map { (presentation, salePrice) ->
            val breakdown = ProfitCalculator.calculate(
                salePrice = salePrice,
                baseCost = baseCost,
                costStructure = costStructure,
                marketplace = marketplace,
                presentation = presentation,
                productId = productId,
                publicationType = publicationType,
                referralKitCost = referralKitCost,
                productWeight = productWeight,
            )
            presentation to breakdown
        }.sortedByDescending { it.second.profitPerPiece?.cents ?: Long.MIN_VALUE }

        return PresentationComparisonResult(
            breakdowns = breakdowns,
            bestByProfitPerPiece = breakdowns.firstOrNull()?.first,
        )
    }
}
