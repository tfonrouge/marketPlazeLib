import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.MarketplaceComparator
import com.fonrouge.commonLib.service.PriceSuggester
import com.fonrouge.commonLib.service.ProfitCalculator
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PricingServiceTest {
    private val marketplaceId: MarketplaceId = OId()

    private val basicCostStructure = CostStructure(
        marketplaceId = marketplaceId,
        name = "Shopify Basic",
        commissions = listOf(
            Commission(name = "Transaction Fee", percentage = 2.9, fixedAmount = Money(30)),
        ),
        fees = listOf(
            Fee(name = "Monthly Plan", amount = Money(2900), type = "fixed"),
        ),
    )

    @Test
    fun `ProfitCalculator computes correct breakdown`() {
        val salePrice = Money(10000) // $100.00
        val baseCost = Money(4000)   // $40.00

        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = baseCost,
            costStructure = basicCostStructure,
        )

        assertEquals(salePrice, breakdown.salePrice)
        assertEquals(baseCost, breakdown.baseCost)
        assertTrue(breakdown.lineItems.isNotEmpty())

        // Commission: 10000 * 2.9% = 290 + 30 fixed = 320
        val commissionItem = breakdown.lineItems.first { it.type == "commission" }
        assertEquals(Money(320), commissionItem.amount)

        // Fee: 2900
        val feeItem = breakdown.lineItems.first { it.type == "fee" }
        assertEquals(Money(2900), feeItem.amount)

        // Total costs: 4000 (base) + 320 (commission) + 2900 (fee) = 7220
        assertEquals(Money(7220), breakdown.totalCosts)

        // Net profit: 10000 - 7220 = 2780
        assertEquals(Money(2780), breakdown.netProfit)

        // Margin: 2780/10000 = 27.8%
        assertEquals(27.8, breakdown.profitMarginPercent, 0.01)
    }

    @Test
    fun `MarketplaceComparator ranks marketplaces correctly`() {
        val cheaperStructure = CostStructure(
            marketplaceId = OId(),
            name = "Cheaper Marketplace",
            commissions = listOf(
                Commission(name = "Fee", percentage = 1.0),
            ),
        )

        val expensiveStructure = CostStructure(
            marketplaceId = OId(),
            name = "Expensive Marketplace",
            commissions = listOf(
                Commission(name = "Fee", percentage = 15.0),
            ),
            fees = listOf(
                Fee(name = "Listing Fee", amount = Money(500)),
            ),
        )

        val result = MarketplaceComparator.compare(
            salePrice = Money(10000),
            baseCost = Money(4000),
            costStructures = listOf(cheaperStructure, expensiveStructure),
        )

        assertEquals(2, result.breakdowns.size)
        assertNotNull(result.bestByProfit)
        assertEquals(cheaperStructure.marketplaceId, result.bestByProfit!!.marketplaceId)
    }

    @Test
    fun `PriceSuggester calculates minimum price for target margin`() {
        val costStructure = CostStructure(
            marketplaceId = marketplaceId,
            name = "Test",
            commissions = listOf(
                Commission(name = "Fee", percentage = 10.0),
            ),
        )
        val baseCost = Money(5000) // $50.00

        // With 10% commission and 20% target margin, denominator = 1 - 0.1 - 0.2 = 0.7
        // minPrice = 5000 / 0.7 = 7142
        val minPrice = PriceSuggester.minimumPriceForMargin(
            baseCost = baseCost,
            costStructure = costStructure,
            targetMarginPercent = 20.0,
        )

        assertEquals(Money(7142), minPrice)
    }

    @Test
    fun `PriceSuggester competitive price strategies`() {
        val now = Clock.System.now()
        val prices = listOf(
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "A", price = Money(10000), observedAt = now),
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "B", price = Money(12000), observedAt = now),
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "C", price = Money(8000), observedAt = now),
        )

        val lowest = PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.MATCH_LOWEST)
        assertEquals(Money(8000), lowest)

        val average = PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.MATCH_AVERAGE)
        assertEquals(Money(10000), average)

        val belowAvg = PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.BELOW_AVERAGE)
        assertEquals(Money(9500), belowAvg)
    }
}
