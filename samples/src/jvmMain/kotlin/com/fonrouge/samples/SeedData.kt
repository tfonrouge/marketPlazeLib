package com.fonrouge.samples

import com.fonrouge.commonLib.model.*
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
        val mercadoLibre = Marketplace(
            name = "MercadoLibre MX",
            type = "MercadoLibre",
            connectionConfig = mapOf(
                "clientId" to "demo_client_id",
                "clientSecret" to "demo_client_secret",
            ),
            enabled = true,
        )
        val amazon = Marketplace(
            name = "Amazon MX",
            type = "Amazon",
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
            variants = listOf(
                ProductVariant(sku = "WIDGET-001-S", name = "Small", price = Money(9900), inventory = 50),
                ProductVariant(sku = "WIDGET-001-L", name = "Large", price = Money(12900), inventory = 30),
            ),
            images = listOf(ProductImage(url = "https://via.placeholder.com/200", altText = "Widget")),
        )
        val gadget = Product(
            sku = "GADGET-002",
            name = "Super Gadget",
            baseCost = Money(8000),
            variants = listOf(
                ProductVariant(sku = "GADGET-002-STD", name = "Standard", price = Money(15900), inventory = 100),
            ),
        )
        val cable = Product(
            sku = "CABLE-003",
            name = "USB-C Cable 2m",
            baseCost = Money(800),
        )
        collections.ProductColl.seed(listOf(widget, gadget, cable))

        // --- Listings ---
        val listings = listOf(
            MarketplaceListing(productId = widget._id, marketplaceId = shopify._id, externalId = "gid://shopify/Product/1001", title = widget.name, price = Money(9900), publishEnabled = true),
            MarketplaceListing(productId = widget._id, marketplaceId = mercadoLibre._id, externalId = "MLM-1001", title = widget.name, price = Money(10500), publishEnabled = true),
            MarketplaceListing(productId = gadget._id, marketplaceId = shopify._id, externalId = "gid://shopify/Product/1002", title = gadget.name, price = Money(15900), publishEnabled = true),
            MarketplaceListing(productId = gadget._id, marketplaceId = mercadoLibre._id, externalId = "MLM-1002", title = gadget.name, price = Money(16900), publishEnabled = false),
            MarketplaceListing(productId = cable._id, marketplaceId = shopify._id, externalId = "gid://shopify/Product/1003", title = cable.name, price = Money(2500), publishEnabled = true),
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
            name = "MercadoLibre Clasica",
            commissions = listOf(Commission(name = "Comision por venta", percentage = 16.0)),
            fees = listOf(Fee(name = "Envio gratis (absorcion)", amount = Money(5000))),
            publicationModalities = listOf(
                PublicationModality(name = "Clasica", fee = Money.ZERO, features = listOf("Exposicion media")),
                PublicationModality(name = "Premium", fee = Money(500), features = listOf("Maxima exposicion", "Envio gratis", "12 MSI")),
            ),
        )
        collections.CostStructureColl.seed(listOf(shopifyCosts, mlCosts))

        // --- Cost Breakdowns (pre-calculated) ---
        val products = listOf(widget, gadget, cable)
        val costStructures = listOf(shopifyCosts, mlCosts)
        val breakdowns = listings.mapNotNull { listing ->
            val product = products.find { it._id == listing.productId } ?: return@mapNotNull null
            val costStructure = costStructures.find { it.marketplaceId == listing.marketplaceId } ?: return@mapNotNull null
            ProfitCalculator.calculate(
                salePrice = listing.price,
                baseCost = product.baseCost,
                costStructure = costStructure,
                productId = product._id,
                listingId = listing._id,
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

        println("Seed data loaded: 3 marketplaces, 3 products, 5 listings, 2 cost structures, ${breakdowns.size} breakdowns, 4 competitor prices")
    }
}
