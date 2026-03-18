import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.PresentationComparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PresentationComparatorTest {

    private val marketplace = Marketplace(
        name = "ML MX", type = "MercadoLibre",
        ivaRate = 0.08, isrRate = 0.025, ivaIncluded = true,
        commissionRateClassic = 0.16,
        commissionRatePremium = 0.205,
    )
    private val costStructure = CostStructure(
        marketplaceId = marketplace._id,
        name = "ML",
    )

    @Test
    fun `compare 3 presentations ordered by profitPerPiece`() {
        val inputs = listOf(
            PresentationComparator.PresentationInput(
                presentation = ProductPresentation(label = "Individual", piecesPerPackage = 1, packagesPerDisplay = 1),
                salePrice = Money(15000), // $150
            ),
            PresentationComparator.PresentationInput(
                presentation = ProductPresentation(label = "Pack x6", piecesPerPackage = 6, packagesPerDisplay = 1),
                salePrice = Money(70000), // $700
            ),
            PresentationComparator.PresentationInput(
                presentation = ProductPresentation(label = "Pack x12", piecesPerPackage = 12, packagesPerDisplay = 1),
                salePrice = Money(120000), // $1200
            ),
        )

        val result = PresentationComparator.compare(
            inputs = inputs,
            baseCost = Money(5000), // $50 per piece
            costStructure = costStructure,
            marketplace = marketplace,
        )

        assertEquals(3, result.breakdowns.size)
        assertNotNull(result.bestByProfitPerPiece)

        // All should have profitPerPiece calculated
        for ((_, breakdown) in result.breakdowns) {
            assertNotNull(breakdown.profitPerPiece)
        }

        // Results should be sorted descending by profitPerPiece
        val profits = result.breakdowns.map { it.second.profitPerPiece!!.cents }
        assertTrue(profits == profits.sortedDescending(), "Should be sorted by profitPerPiece descending")
    }

    @Test
    fun `hypothetical presentations included`() {
        val inputs = listOf(
            PresentationComparator.PresentationInput(
                presentation = ProductPresentation(label = "Real", piecesPerPackage = 1, packagesPerDisplay = 1),
                salePrice = Money(10000),
            ),
            PresentationComparator.PresentationInput(
                presentation = ProductPresentation(label = "Hypothetical", piecesPerPackage = 10, packagesPerDisplay = 1, isHypothetical = true),
                salePrice = Money(80000),
            ),
        )

        val result = PresentationComparator.compare(
            inputs = inputs,
            baseCost = Money(3000),
            costStructure = costStructure,
            marketplace = marketplace,
        )

        assertEquals(2, result.breakdowns.size)
        assertTrue(result.breakdowns.any { it.first.isHypothetical })
    }
}
