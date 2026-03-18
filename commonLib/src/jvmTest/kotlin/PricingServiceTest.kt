import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.*
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
    fun `ProfitCalculator computes correct breakdown without taxes`() {
        val salePrice = Money(10000)
        val baseCost = Money(4000)

        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = baseCost,
            costStructure = basicCostStructure,
        )

        assertEquals(salePrice, breakdown.salePrice)
        assertEquals(salePrice, breakdown.finalPrice) // no discount
        assertEquals(baseCost, breakdown.baseCost)

        val commissionItem = breakdown.lineItems.first { it.type == "commission" }
        assertEquals(Money(320), commissionItem.amount)

        val feeItem = breakdown.lineItems.first { it.type == "fee" }
        assertEquals(Money(2900), feeItem.amount)

        assertEquals(Money(7220), breakdown.totalCosts)
        assertEquals(Money(2780), breakdown.netProfit)
        assertEquals(27.8, breakdown.profitMarginPercent, 0.01)
        assertEquals("HEALTHY", breakdown.healthLevel)
    }

    @Test
    fun `ProfitCalculator with MX tax withholdings`() {
        val mlMarketplace = Marketplace(
            name = "MercadoLibre MX", type = "MercadoLibre",
            ivaRate = 0.08, isrRate = 0.025, ivaIncluded = true,
        )
        val mlCostStructure = CostStructure(
            marketplaceId = mlMarketplace._id,
            name = "ML Clasica",
            commissions = listOf(Commission(name = "Comision", percentage = 16.0)),
            shippingTiers = listOf(
                ShippingTier(name = "Bajo", minValue = 0.0, maxValue = 299.0, cost = Money(3700)),
                ShippingTier(name = "Gratis", minValue = 299.0, maxValue = Double.MAX_VALUE, cost = Money.ZERO),
            ),
        )

        val salePrice = Money(50000) // $500 MXN
        val baseCost = Money(20000)  // $200 MXN

        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = baseCost,
            costStructure = mlCostStructure,
            marketplace = mlMarketplace,
        )

        // taxableBase = 50000 / 1.16 = 43103
        assertEquals(Money(43103), breakdown.taxableBase)

        // IVA = 43103 * 0.08 = 3448
        assertEquals(Money(3448), breakdown.ivaWithheld)

        // ISR = 43103 * 0.025 = 1077
        assertEquals(Money(1077), breakdown.isrWithheld)

        // Commission = 50000 * 16% = 8000
        val commission = breakdown.lineItems.first { it.type == "commission" }
        assertEquals(Money(8000), commission.amount)

        // Shipping = $500 > $299 → free
        assertEquals(Money.ZERO, breakdown.shippingCost)

        // Total = 20000 + 8000 + 3448 + 1077 = 32525
        assertEquals(Money(32525), breakdown.totalCosts)

        // Profit = 50000 - 32525 = 17475
        assertEquals(Money(17475), breakdown.netProfit)

        // Margin = 17475 / 50000 = 34.95%
        assertTrue(breakdown.profitMarginPercent > 34.0)
        assertEquals("EXCELLENT", breakdown.healthLevel)
    }

    @Test
    fun `ProfitCalculator with discount`() {
        val salePrice = Money(10000) // $100
        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = Money(4000),
            costStructure = basicCostStructure,
            discount = 0.10, // 10% off
        )
        assertEquals(Money(9000), breakdown.finalPrice) // $90 after discount
        assertEquals(salePrice, breakdown.salePrice) // original price preserved
    }

    @Test
    fun `ProfitCalculator with presentation per-piece metrics`() {
        val presentation = ProductPresentation(
            label = "Display 10x50",
            piecesPerPackage = 50,
            packagesPerDisplay = 10,
            packagingCost = Money(500),
        )
        assertEquals(500, presentation.totalPieces)

        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(100000), // $1000
            baseCost = Money(40000),   // $400
            costStructure = basicCostStructure,
            presentation = presentation,
        )

        assertEquals(500, breakdown.piecesCount)
        assertNotNull(breakdown.pricePerPiece)
        assertNotNull(breakdown.costPerPiece)
        assertNotNull(breakdown.profitPerPiece)
        assertEquals(Money(200), breakdown.pricePerPiece) // 100000/500
    }

    @Test
    fun `ProfitCalculator scales baseCost by totalPieces`() {
        val presentation = ProductPresentation(
            label = "Pack x6",
            piecesPerPackage = 6,
            packagesPerDisplay = 1,
            packagingCost = Money(100),
        )
        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(60000), // $600
            baseCost = Money(5000),   // $50 per piece
            costStructure = CostStructure(marketplaceId = marketplaceId, name = "None"),
            presentation = presentation,
        )
        // scaledBaseCost = 5000 * 6 = 30000
        // totalCosts = 30000 + 100 (packaging) = 30100
        assertEquals(Money(30100), breakdown.totalCosts)
    }

    @Test
    fun `ProfitCalculator with marketplace dual commission rates`() {
        val mlMarketplace = Marketplace(
            name = "ML MX", type = "MercadoLibre",
            commissionRateClassic = 0.16,
            commissionRatePremium = 0.205,
        )
        val costStructure = CostStructure(marketplaceId = mlMarketplace._id, name = "ML")

        val classicBreakdown = ProfitCalculator.calculate(
            salePrice = Money(50000),
            baseCost = Money(20000),
            costStructure = costStructure,
            marketplace = mlMarketplace,
            publicationType = PublicationType.CLASICA,
        )

        val premiumBreakdown = ProfitCalculator.calculate(
            salePrice = Money(50000),
            baseCost = Money(20000),
            costStructure = costStructure,
            marketplace = mlMarketplace,
            publicationType = PublicationType.PREMIUM,
        )

        val classicComm = classicBreakdown.lineItems.first { it.type == "commission" }
        val premiumComm = premiumBreakdown.lineItems.first { it.type == "commission" }

        // Classic: 50000 * 0.16 = 8000
        assertEquals(Money(8000), classicComm.amount)
        // Premium: 50000 * 0.205 = 10250
        assertEquals(Money(10250), premiumComm.amount)
        assertTrue(premiumBreakdown.netProfit < classicBreakdown.netProfit)
    }

    @Test
    fun `ProfitCalculator with cargo fijo from marketplace tiers`() {
        val marketplace = Marketplace(
            name = "ML", type = "MercadoLibre",
            cargoFijoTiers = listOf(
                CargoFijoTier(minPrice = 0.0, maxPrice = 99.0, cost = Money(2500)),
                CargoFijoTier(minPrice = 99.0, maxPrice = 299.0, cost = Money(3700)),
                CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
            ),
        )
        val costStructure = CostStructure(marketplaceId = marketplace._id, name = "ML")

        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(15000), // $150 → [99, 299) = $37
            baseCost = Money(5000),
            costStructure = costStructure,
            marketplace = marketplace,
        )

        assertEquals(Money(3700), breakdown.cargoFijo)
    }

    @Test
    fun `ProfitCalculator with cargo fijo override`() {
        val marketplace = Marketplace(
            name = "ML", type = "MercadoLibre",
            cargoFijoTiers = listOf(
                CargoFijoTier(minPrice = 0.0, maxPrice = 299.0, cost = Money(3700)),
                CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
            ),
        )
        val costStructure = CostStructure(marketplaceId = marketplace._id, name = "ML")

        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(15000),
            baseCost = Money(5000),
            costStructure = costStructure,
            marketplace = marketplace,
            cargoFijoOverride = Money(1500),
        )

        assertEquals(Money(1500), breakdown.cargoFijo)
    }

    @Test
    fun `ProfitCalculator with envio logistico`() {
        val marketplace = Marketplace(
            name = "ML", type = "MercadoLibre",
            envioTiers = listOf(
                EnvioTier(minWeight = 0.0, maxWeight = 1.0, cost = Money(14900)),
                EnvioTier(minWeight = 1.0, maxWeight = 2.0, cost = Money(16900)),
            ),
            envioDiscounts = listOf(
                EnvioDiscount(minPrice = 0.0, maxPrice = 299.0, discountRate = 0.30),
                EnvioDiscount(minPrice = 299.0, maxPrice = null, discountRate = 0.50),
            ),
        )
        val costStructure = CostStructure(marketplaceId = marketplace._id, name = "ML")

        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(50000), // $500 → 50% discount
            baseCost = Money(20000),
            costStructure = costStructure,
            marketplace = marketplace,
            productWeight = 1.5, // → tier [1, 2) = 16900, discounted 50% = 8450
        )

        assertEquals(Money(8450), breakdown.costoEnvioLogistico)
    }

    @Test
    fun `ProfitCalculator with referralKitCost`() {
        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(10000),
            baseCost = Money(4000),
            costStructure = CostStructure(marketplaceId = marketplaceId, name = "None"),
            referralKitCost = Money(500),
        )
        // totalCosts = 4000 (baseCost) + 500 (referralKit) = 4500
        assertEquals(Money(4500), breakdown.totalCosts)
        assertEquals(Money(500), breakdown.referralKitCost)
    }

    @Test
    fun `ProfitCalculator with envioCliente`() {
        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(10000),
            baseCost = Money(4000),
            costStructure = CostStructure(marketplaceId = marketplaceId, name = "None"),
            envioCliente = Money(3000),
        )
        val envioItem = breakdown.lineItems.find { it.type == "envio_cliente" }
        assertNotNull(envioItem)
        assertEquals(Money(3000), envioItem.amount)
        assertEquals(Money(3000), breakdown.envioCliente)
    }

    @Test
    fun `HealthLevel NO_DATA when baseCost is zero`() {
        assertEquals(HealthLevel.NO_DATA, HealthLevel.classify(25.0, Money.ZERO))
        assertEquals(HealthLevel.NO_DATA, HealthLevel.classify(50.0, Money(-100)))
        // With positive baseCost, normal classification applies
        assertEquals(HealthLevel.HEALTHY, HealthLevel.classify(25.0, Money(100)))
    }

    @Test
    fun `ProfitCalculator returns NO_DATA health when baseCost zero`() {
        val breakdown = ProfitCalculator.calculate(
            salePrice = Money(10000),
            baseCost = Money.ZERO,
            costStructure = CostStructure(marketplaceId = marketplaceId, name = "None"),
        )
        assertEquals("NO_DATA", breakdown.healthLevel)
    }

    @Test
    fun `resolveShippingCost finds correct tier`() {
        val tiers = listOf(
            ShippingTier(name = "Bajo", minValue = 0.0, maxValue = 99.0, cost = Money(2500)),
            ShippingTier(name = "Medio", minValue = 99.0, maxValue = 299.0, cost = Money(3700)),
            ShippingTier(name = "Gratis", minValue = 299.0, maxValue = Double.MAX_VALUE, cost = Money.ZERO),
        )
        assertEquals(Money(2500), resolveShippingCost(Money(5000), tiers))   // $50 → $25
        assertEquals(Money(3700), resolveShippingCost(Money(15000), tiers))  // $150 → $37
        assertEquals(Money.ZERO, resolveShippingCost(Money(50000), tiers))   // $500 → free
    }

    @Test
    fun `HealthLevel classification`() {
        assertEquals(HealthLevel.CRITICAL, HealthLevel.classify(5.0))
        assertEquals(HealthLevel.ACCEPTABLE, HealthLevel.classify(12.0))
        assertEquals(HealthLevel.HEALTHY, HealthLevel.classify(20.0))
        assertEquals(HealthLevel.EXCELLENT, HealthLevel.classify(35.0))
        assertEquals(HealthLevel.CRITICAL, HealthLevel.classify(-5.0))
    }

    @Test
    fun `MarketplaceComparator ranks marketplaces correctly`() {
        val cheapMarketplace = Marketplace(name = "Cheap", type = "Test")
        val cheapStructure = CostStructure(
            marketplaceId = cheapMarketplace._id, name = "Cheap",
            commissions = listOf(Commission(name = "Fee", percentage = 1.0)),
        )
        val expensiveMarketplace = Marketplace(
            name = "Expensive", type = "Test",
            ivaRate = 0.08, isrRate = 0.025,
        )
        val expensiveStructure = CostStructure(
            marketplaceId = expensiveMarketplace._id, name = "Expensive",
            commissions = listOf(Commission(name = "Fee", percentage = 15.0)),
            fees = listOf(Fee(name = "Listing Fee", amount = Money(500))),
        )

        val result = MarketplaceComparator.compare(
            salePrice = Money(10000),
            baseCost = Money(4000),
            pairs = listOf(
                MarketplaceComparator.MarketplaceCostPair(cheapMarketplace, cheapStructure),
                MarketplaceComparator.MarketplaceCostPair(expensiveMarketplace, expensiveStructure),
            ),
        )

        assertEquals(2, result.breakdowns.size)
        assertNotNull(result.bestByProfit)
        assertEquals(cheapStructure.marketplaceId, result.bestByProfit?.marketplaceId)
    }

    @Test
    fun `PriceSuggester calculates minimum price for target margin`() {
        val costStructure = CostStructure(
            marketplaceId = marketplaceId, name = "Test",
            commissions = listOf(Commission(name = "Fee", percentage = 10.0)),
        )
        val minPrice = PriceSuggester.minimumPriceForMargin(
            baseCost = Money(5000),
            costStructure = costStructure,
            targetMarginPercent = 20.0,
        )
        assertEquals(Money(7142), minPrice)
    }

    @Test
    fun `PriceSuggester with MX taxes raises minimum price`() {
        val mlMarketplace = Marketplace(
            name = "ML", type = "MercadoLibre",
            ivaRate = 0.08, isrRate = 0.025, ivaIncluded = true,
        )
        val costStructure = CostStructure(
            marketplaceId = mlMarketplace._id, name = "ML",
            commissions = listOf(Commission(name = "Fee", percentage = 10.0)),
        )
        val minPriceNoTax = PriceSuggester.minimumPriceForMargin(
            baseCost = Money(5000), costStructure = costStructure, targetMarginPercent = 20.0,
        )
        val minPriceWithTax = PriceSuggester.minimumPriceForMargin(
            baseCost = Money(5000), costStructure = costStructure,
            marketplace = mlMarketplace, targetMarginPercent = 20.0,
        )
        assertTrue(minPriceWithTax > minPriceNoTax, "Tax withholdings should raise minimum price")
    }

    @Test
    fun `PriceSuggester competitive price strategies`() {
        val now = Clock.System.now()
        val prices = listOf(
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "A", price = Money(10000), observedAt = now),
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "B", price = Money(12000), observedAt = now),
            CompetitorPrice(marketplaceId = marketplaceId, competitorName = "C", price = Money(8000), observedAt = now),
        )

        assertEquals(Money(8000), PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.MATCH_LOWEST))
        assertEquals(Money(10000), PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.MATCH_AVERAGE))
        assertEquals(Money(9500), PriceSuggester.competitivePrice(prices, PriceSuggester.PriceStrategy.BELOW_AVERAGE))
    }

    @Test
    fun `MarketplaceDefaults ML Mexico has correct configuration`() {
        val ml = MarketplaceDefaults.mercadoLibreMexico()
        assertEquals(0.16, ml.commissionRateClassic)
        assertEquals(0.205, ml.commissionRatePremium)
        assertEquals(0.08, ml.ivaRate)
        assertEquals(0.025, ml.isrRate)
        assertTrue(ml.ivaIncluded)
        assertTrue(ml.isDefault)
        assertEquals(Money(3500), ml.minimumPrice)
        assertEquals(4, ml.cargoFijoTiers.size)
        assertEquals(21, ml.envioTiers.size)
        assertEquals(3, ml.envioDiscounts.size)
    }

    @Test
    fun `MarketplaceDefaults Amazon Mexico has correct configuration`() {
        val amz = MarketplaceDefaults.amazonMexico()
        assertEquals(0.15, amz.commissionRateClassic)
        assertEquals(0.15, amz.commissionRatePremium)
        assertEquals(0.08, amz.ivaRate)
        assertEquals(0.025, amz.isrRate)
        assertTrue(amz.isDefault)
        assertEquals(4, amz.cargoFijoTiers.size)
        assertTrue(amz.envioTiers.isEmpty())
        assertTrue(amz.envioDiscounts.isEmpty())
    }
}
