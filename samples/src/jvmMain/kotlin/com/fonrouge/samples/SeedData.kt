package com.fonrouge.samples

import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.MarketplaceDefaults
import com.fonrouge.commonLib.service.ProfitCalculator
import com.fonrouge.samples.collections.SampleCollections
import kotlinx.datetime.Clock

/**
 * Seeds the in-memory repositories with sample data for demonstration.
 * Uses InMemoryRepository.seed() — synchronous, no MongoDB required.
 */
object SeedData {

    fun seed(collections: SampleCollections) {

        // --- Marketplaces ---
        val shopify = Marketplace(
            name = "My Shopify Store",
            type = "Shopify",
            connectionConfig = mapOf(
                "url" to "mystore.myshopify.com",
                "apiVersion" to "2025-10",
                "accessToken" to "shpat_demo_token",
            ),
            enabled = true,
        )
        val mercadoLibre = MarketplaceDefaults.mercadoLibreMexico().copy(
            connectionConfig = mapOf(
                "clientId" to "demo_client_id",
                "clientSecret" to "demo_client_secret",
            ),
            enabled = true,
        )
        val amazon = MarketplaceDefaults.amazonMexico().copy(
            connectionConfig = mapOf(
                "sellerId" to "demo_seller",
                "mwsAuthToken" to "demo_token",
            ),
            enabled = false,
        )
        collections.MarketplaceColl.seed(listOf(shopify, mercadoLibre, amazon))

        // --- Products ---
        val widget = Product(
            sku = "WIDGET-001",
            name = "Premium Widget",
            baseCost = Money(5000),
            referralKitCost = Money(200),
            variants = listOf(
                ProductVariant(sku = "WIDGET-001-S", name = "Small", price = Money(9900), inventory = 50, options = mapOf("Size" to "Small")),
                ProductVariant(sku = "WIDGET-001-L", name = "Large", price = Money(12900), inventory = 30, options = mapOf("Size" to "Large")),
            ),
            images = listOf(ProductImage(url = "https://via.placeholder.com/200", altText = "Widget", position = 0)),
            presentations = listOf(
                ProductPresentation(
                    label = "Individual",
                    piecesPerPackage = 1,
                    packagesPerDisplay = 1,
                    packagingCost = Money(50),
                    packageWeight = 0.5,
                    packageLength = 15.0,
                    packageWidth = 10.0,
                    packageHeight = 5.0,
                ),
                ProductPresentation(
                    label = "Pack x6",
                    piecesPerPackage = 6,
                    packagesPerDisplay = 1,
                    packagingCost = Money(200),
                    packageWeight = 3.2,
                    packageLength = 30.0,
                    packageWidth = 20.0,
                    packageHeight = 15.0,
                ),
                ProductPresentation(
                    label = "Display 10x6 (hipotético)",
                    piecesPerPackage = 6,
                    packagesPerDisplay = 10,
                    packagingCost = Money(500),
                    isHypothetical = true,
                    packageWeight = 18.0,
                    packageLength = 60.0,
                    packageWidth = 40.0,
                    packageHeight = 30.0,
                ),
            ),
            erpSourceId = "ERP-W001",
            priceLists = listOf(
                PriceListEntry(name = "Retail", price = Money(9900)),
                PriceListEntry(name = "Wholesale", price = Money(7500), compareAtPrice = Money(9900)),
            ),
            inventoryQuantity = 80,
            descriptionHtml = "<p>Premium quality widget for everyday use.</p>",
            publishEnabled = true,
            status = "active",
            tags = listOf("widget", "premium", "electronics"),
            weight = 0.5,
            weightUnit = "kg",
            barcode = "7501234567890",
            seoTitle = "Premium Widget | My Store",
            seoDescription = "Best premium widget available online",
        )
        val gadget = Product(
            sku = "GADGET-002",
            name = "Super Gadget",
            baseCost = Money(8000),
            variants = listOf(
                ProductVariant(sku = "GADGET-002-STD", name = "Standard", price = Money(15900), inventory = 100),
            ),
            presentations = listOf(
                ProductPresentation(
                    label = "Individual",
                    piecesPerPackage = 1,
                    packagesPerDisplay = 1,
                    packageWeight = 1.2,
                    packageLength = 20.0,
                    packageWidth = 15.0,
                    packageHeight = 10.0,
                ),
            ),
            erpSourceId = "ERP-G002",
            priceLists = listOf(PriceListEntry(name = "Retail", price = Money(15900))),
            inventoryQuantity = 100,
            publishEnabled = true,
            status = "active",
            tags = listOf("gadget", "tech"),
            weight = 1.2,
        )
        val cable = Product(
            sku = "CABLE-003",
            name = "USB-C Cable 2m",
            baseCost = Money(800),
            publishEnabled = false,
            status = "draft",
            weight = 0.1,
        )
        collections.ProductColl.seed(listOf(widget, gadget, cable))

        // --- Listings ---
        val listings = listOf(
            MarketplaceListing(
                productId = widget._id, marketplaceId = shopify._id,
                externalId = "gid://shopify/Product/1001", title = widget.name,
                price = Money(9900), publishEnabled = true,
            ),
            MarketplaceListing(
                productId = widget._id, marketplaceId = mercadoLibre._id,
                externalId = "MLM-1001", title = widget.name,
                price = Money(10500), publishEnabled = true,
                presentationIndex = 0,
                publicationType = PublicationType.CLASICA,
            ),
            MarketplaceListing(
                productId = widget._id, marketplaceId = mercadoLibre._id,
                externalId = "MLM-1001-PACK", title = "${widget.name} Pack x6",
                price = Money(55000), publishEnabled = true,
                presentationIndex = 1,
                publicationType = PublicationType.PREMIUM,
            ),
            MarketplaceListing(
                productId = gadget._id, marketplaceId = shopify._id,
                externalId = "gid://shopify/Product/1002", title = gadget.name,
                price = Money(15900), publishEnabled = true,
            ),
            MarketplaceListing(
                productId = gadget._id, marketplaceId = mercadoLibre._id,
                externalId = "MLM-1002", title = gadget.name,
                price = Money(16900), publishEnabled = false,
                publicationType = PublicationType.CLASICA,
            ),
            MarketplaceListing(
                productId = cable._id, marketplaceId = shopify._id,
                externalId = "gid://shopify/Product/1003", title = cable.name,
                price = Money(2500), publishEnabled = true,
            ),
        )
        collections.MarketplaceListingColl.seed(listings)

        // --- Cost Structures ---
        val shopifyCosts = CostStructure(
            marketplaceId = shopify._id,
            name = "Shopify Basic Plan",
            commissions = listOf(Commission(name = "Transaction Fee", percentage = 2.9, fixedAmount = Money(30))),
            fees = listOf(Fee(name = "Monthly Plan (prorated)", amount = Money(2900))),
        )
        val mlCosts = CostStructure(
            marketplaceId = mercadoLibre._id,
            name = "MercadoLibre (via marketplace model)",
            commissions = emptyList(),
        )
        collections.CostStructureColl.seed(listOf(shopifyCosts, mlCosts))

        // --- Cost Breakdowns (pre-calculated) ---
        val products = listOf(widget, gadget, cable)
        val marketplaces = listOf(shopify, mercadoLibre, amazon)
        val costStructures = listOf(shopifyCosts, mlCosts)
        val breakdowns = listings.mapNotNull { listing ->
            val product = products.find { it._id == listing.productId } ?: return@mapNotNull null
            val costStructure = costStructures.find { it.marketplaceId == listing.marketplaceId } ?: return@mapNotNull null
            val marketplace = marketplaces.find { it._id == listing.marketplaceId } ?: return@mapNotNull null
            val presentation = listing.presentationIndex?.let { product.presentations.getOrNull(it) }
            ProfitCalculator.calculate(
                salePrice = listing.price,
                baseCost = product.baseCost,
                costStructure = costStructure,
                marketplace = marketplace,
                productId = product._id,
                listingId = listing._id,
                presentation = presentation,
                publicationType = listing.publicationType,
                referralKitCost = product.referralKitCost,
                productWeight = product.weight ?: 0.0,
                cargoFijoOverride = listing.cargoFijoOverride,
                envioOverride = listing.envioOverride,
            )
        }
        collections.CostBreakdownColl.seed(breakdowns)

        // --- Competitor Prices ---
        val now = Clock.System.now()
        val competitors = listOf(
            CompetitorPrice(marketplaceId = mercadoLibre._id, productId = widget._id, competitorName = "VendedorA", price = Money(8900), observedAt = now),
            CompetitorPrice(marketplaceId = mercadoLibre._id, productId = widget._id, competitorName = "VendedorB", price = Money(11200), observedAt = now),
            CompetitorPrice(marketplaceId = mercadoLibre._id, productId = gadget._id, competitorName = "TechShopMX", price = Money(14500), observedAt = now),
            CompetitorPrice(marketplaceId = shopify._id, productId = widget._id, competitorName = "WidgetsDirect", price = Money(9500), observedAt = now),
        )
        collections.CompetitorPriceColl.seed(competitors)

        println("Seed data loaded: 3 marketplaces, 3 products, ${listings.size} listings, 2 cost structures, ${breakdowns.size} breakdowns, 4 competitor prices")
    }
}
