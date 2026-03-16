import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.MarketplaceComparator
import com.fonrouge.commonLib.service.PriceSuggester
import com.fonrouge.commonLib.service.ProfitCalculator
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests demonstrating how to use the marketPlazeLib library.
 * These tests exercise models, filters, pricing services, and serialization
 * without requiring MongoDB or a running server.
 */
class SampleIntegrationTest {

    // --- Sample Data ---

    private val shopifyMarketplace = Marketplace(
        name = "My Shopify Store",
        type = "Shopify",
        connectionConfig = mapOf(
            "url" to "mystore.myshopify.com",
            "apiVersion" to "2025-10",
            "accessToken" to "shpat_xxxx",
        ),
        enabled = true,
    )

    private val mlMarketplace = Marketplace(
        name = "MercadoLibre MX",
        type = "MercadoLibre",
        connectionConfig = mapOf(
            "clientId" to "xxx",
            "clientSecret" to "yyy",
            "refreshToken" to "zzz",
        ),
        enabled = true,
    )

    private val sampleProduct = Product(
        sku = "WIDGET-001",
        name = "Premium Widget",
        baseCost = Money(5000), // $50.00
        variants = listOf(
            ProductVariant(sku = "WIDGET-001-S", name = "Small", price = Money(9900), inventory = 50),
            ProductVariant(sku = "WIDGET-001-L", name = "Large", price = Money(12900), inventory = 30),
        ),
        images = listOf(
            ProductImage(url = "https://cdn.example.com/widget.jpg", altText = "Widget"),
        ),
    )

    private val shopifyCostStructure = CostStructure(
        marketplaceId = shopifyMarketplace._id,
        name = "Shopify Basic Plan",
        commissions = listOf(
            Commission(name = "Transaction Fee", percentage = 2.9, fixedAmount = Money(30)),
        ),
        fees = listOf(
            Fee(name = "Monthly Plan (prorated)", amount = Money(2900)),
        ),
    )

    private val mlCostStructure = CostStructure(
        marketplaceId = mlMarketplace._id,
        name = "MercadoLibre Clasica",
        commissions = listOf(
            Commission(name = "Comision por venta", percentage = 16.0),
        ),
        fees = listOf(
            Fee(name = "Envio gratis (absorcion)", amount = Money(5000)),
        ),
        publicationModalities = listOf(
            PublicationModality(
                name = "Clasica",
                fee = Money.ZERO,
                features = listOf("Exposicion media", "Sin envio gratis"),
            ),
            PublicationModality(
                name = "Premium",
                fee = Money(500),
                features = listOf("Maxima exposicion", "Envio gratis", "12 MSI"),
            ),
        ),
    )

    // --- Model Tests ---

    @Test
    fun `marketplace model creation and serialization`() {
        val json = Json.encodeToString(shopifyMarketplace)
        val decoded = Json.decodeFromString<Marketplace>(json)
        assertEquals(shopifyMarketplace.name, decoded.name)
        assertEquals(shopifyMarketplace.type, decoded.type)
        assertEquals("shpat_xxxx", decoded.connectionConfig["accessToken"])
        assertTrue(decoded.enabled)
    }

    @Test
    fun `product model with variants and images`() {
        assertEquals("WIDGET-001", sampleProduct.sku)
        assertEquals(2, sampleProduct.variants.size)
        assertEquals(Money(9900), sampleProduct.variants[0].price)
        assertEquals(1, sampleProduct.images.size)
    }

    @Test
    fun `marketplace listing links product to marketplace`() {
        val listing = MarketplaceListing(
            productId = sampleProduct._id,
            marketplaceId = shopifyMarketplace._id,
            externalId = "gid://shopify/Product/12345",
            title = sampleProduct.name,
            price = Money(9900),
            publishEnabled = true,
        )
        assertEquals(sampleProduct._id, listing.productId)
        assertEquals(shopifyMarketplace._id, listing.marketplaceId)
        assertTrue(listing.publishEnabled)
    }

    // --- Filter Tests ---

    @Test
    fun `marketplace filter with immutable copy pattern`() {
        val filter = MarketplaceFilter()
        val filtered = filter.copy(type = "Shopify", enabled = true)
        assertEquals("Shopify", filtered.type)
        assertEquals(true, filtered.enabled)
        // Original unchanged
        assertEquals(null, filter.type)
    }

    @Test
    fun `listing filter with marketplace master ID`() {
        val filter = MarketplaceListingFilter(
            masterItemId = shopifyMarketplace._id,
            publishEnabled = true,
        )
        assertEquals(shopifyMarketplace._id, filter.masterItemId)
        assertTrue(filter.publishEnabled!!)
    }

    @Test
    fun `competitor price filter copy pattern`() {
        val filter = CompetitorPriceFilter(masterItemId = mlMarketplace._id)
        val updated = filter.copy(competitorName = "CompetitorA")
        assertEquals("CompetitorA", updated.competitorName)
        assertEquals(mlMarketplace._id, updated.masterItemId)
    }

    // --- Pricing Service Tests ---

    @Test
    fun `profit calculator - Shopify cost breakdown`() {
        val salePrice = Money(9900)
        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = sampleProduct.baseCost,
            costStructure = shopifyCostStructure,
            productId = sampleProduct._id,
        )

        assertEquals(salePrice, breakdown.salePrice)
        assertEquals(sampleProduct.baseCost, breakdown.baseCost)
        assertTrue(breakdown.netProfit.cents > 0, "Should be profitable")
        assertTrue(breakdown.profitMarginPercent > 0, "Should have positive margin")

        // Verify commission: 9900 * 2.9% = 287.1 → 287 + 30 fixed = 317
        val commissionItem = breakdown.lineItems.first { it.type == "commission" }
        assertEquals(Money(317), commissionItem.amount)
    }

    @Test
    fun `profit calculator - MercadoLibre cost breakdown`() {
        val salePrice = Money(9900)
        val breakdown = ProfitCalculator.calculate(
            salePrice = salePrice,
            baseCost = sampleProduct.baseCost,
            costStructure = mlCostStructure,
            productId = sampleProduct._id,
        )

        // Commission: 9900 * 16% = 1584
        val commissionItem = breakdown.lineItems.first { it.type == "commission" }
        assertEquals(Money(1584), commissionItem.amount)

        // Fee: 5000 (shipping absorption)
        val feeItem = breakdown.lineItems.first { it.type == "fee" }
        assertEquals(Money(5000), feeItem.amount)
    }

    @Test
    fun `marketplace comparator - Shopify vs MercadoLibre`() {
        val salePrice = Money(9900)
        val result = MarketplaceComparator.compare(
            salePrice = salePrice,
            baseCost = sampleProduct.baseCost,
            pairs = listOf(
                MarketplaceComparator.MarketplaceCostPair(shopifyMarketplace, shopifyCostStructure),
                MarketplaceComparator.MarketplaceCostPair(mlMarketplace, mlCostStructure),
            ),
            productId = sampleProduct._id,
        )

        assertEquals(2, result.breakdowns.size)
        assertNotNull(result.bestByProfit)
        // Shopify should be more profitable (lower commissions)
        assertEquals(shopifyCostStructure.marketplaceId, result.bestByProfit!!.marketplaceId)
    }

    @Test
    fun `price suggester - minimum price for 20pct margin on Shopify`() {
        val minPrice = PriceSuggester.minimumPriceForMargin(
            baseCost = sampleProduct.baseCost,
            costStructure = shopifyCostStructure,
            targetMarginPercent = 20.0,
        )
        assertTrue(minPrice.cents > sampleProduct.baseCost.cents, "Min price must exceed base cost")

        // Verify: at this price, margin should be ~20%
        val breakdown = ProfitCalculator.calculate(
            salePrice = minPrice,
            baseCost = sampleProduct.baseCost,
            costStructure = shopifyCostStructure,
        )
        assertTrue(breakdown.profitMarginPercent >= 19.0, "Margin should be near target")
    }

    @Test
    fun `price suggester - competitive pricing strategies`() {
        val now = Clock.System.now()
        val competitors = listOf(
            CompetitorPrice(marketplaceId = mlMarketplace._id, competitorName = "SellerA", price = Money(8500), observedAt = now),
            CompetitorPrice(marketplaceId = mlMarketplace._id, competitorName = "SellerB", price = Money(10500), observedAt = now),
            CompetitorPrice(marketplaceId = mlMarketplace._id, competitorName = "SellerC", price = Money(9000), observedAt = now),
        )

        val lowest = PriceSuggester.competitivePrice(competitors, PriceSuggester.PriceStrategy.MATCH_LOWEST)
        assertEquals(Money(8500), lowest)

        val average = PriceSuggester.competitivePrice(competitors, PriceSuggester.PriceStrategy.MATCH_AVERAGE)
        assertEquals(Money(9333), average)
    }

    // --- Money Arithmetic ---

    @Test
    fun `money arithmetic for cost calculations`() {
        val price = Money(9900)
        val cost = Money(5000)
        val commission = Money((price.cents * 0.16).toLong()) // 16% = 1584
        val shipping = Money(5000)

        val totalCost = cost + commission + shipping
        val profit = price - totalCost

        assertEquals(Money(11584), totalCost)
        assertEquals(Money(-1684), profit) // Loss!

        assertTrue(profit < Money.ZERO, "Should be a loss at this commission rate")
    }

    @Test
    fun `money serialization roundtrip`() {
        val money = Money(12345)
        val json = Json.encodeToString(money)
        assertEquals("12345", json) // Value class serializes as raw Long
        val decoded = Json.decodeFromString<Money>(json)
        assertEquals(money, decoded)
    }

    // --- End-to-End Scenario ---

    @Test
    fun `full scenario - register marketplace, calculate profits, compare`() {
        // 1. Register marketplaces (created above)
        assertTrue(shopifyMarketplace.enabled)
        assertTrue(mlMarketplace.enabled)

        // 2. Define product
        val product = sampleProduct
        assertEquals("WIDGET-001", product.sku)

        // 3. Create listings on both marketplaces
        val shopifyListing = MarketplaceListing(
            productId = product._id,
            marketplaceId = shopifyMarketplace._id,
            externalId = "gid://shopify/Product/12345",
            title = product.name,
            price = Money(9900),
            publishEnabled = true,
        )
        val mlListing = MarketplaceListing(
            productId = product._id,
            marketplaceId = mlMarketplace._id,
            externalId = "MLM-123456789",
            title = product.name,
            price = Money(9900),
            publishEnabled = true,
        )

        // 4. Calculate profit on each
        val shopifyBreakdown = ProfitCalculator.calculate(
            salePrice = shopifyListing.price,
            baseCost = product.baseCost,
            costStructure = shopifyCostStructure,
            productId = product._id,
            listingId = shopifyListing._id,
        )
        val mlBreakdown = ProfitCalculator.calculate(
            salePrice = mlListing.price,
            baseCost = product.baseCost,
            costStructure = mlCostStructure,
            productId = product._id,
            listingId = mlListing._id,
        )

        // 5. Compare
        assertTrue(shopifyBreakdown.netProfit > mlBreakdown.netProfit,
            "Shopify should be more profitable for this product")

        // 6. Suggest minimum price for 15% margin on ML
        val mlMinPrice = PriceSuggester.minimumPriceForMargin(
            baseCost = product.baseCost,
            costStructure = mlCostStructure,
            targetMarginPercent = 15.0,
        )
        assertTrue(mlMinPrice > Money(9900),
            "Need higher price on ML to achieve 15% margin")
    }
}
