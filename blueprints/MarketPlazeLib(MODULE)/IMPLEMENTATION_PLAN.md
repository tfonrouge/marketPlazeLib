# MarketPlazeLib — Implementation Plan

## Phase 1: Core Domain (commonLib) ✅
- [x] Money value class with arithmetic operators
- [x] Value types: Commission, Fee, ShippingTier, AdvertisingRate, PublicationModality, FeeLineItem, ProductVariant, ProductImage
- [x] Entity models: Marketplace, Product, MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice
- [x] ICommonContainer for each entity (6 abstract classes)
- [x] Abstract Coll classes (6 JVM collections)
- [x] MarketPlazeCollections factory
- [x] IMarketplaceSync interface
- [x] ProfitCalculator, MarketplaceComparator, PriceSuggester services
- [x] Delete: MarketPlaceId, IMarketProductId, IMarketProduct, IMarketProductColl

## Phase 2: shopifyLib Refactor ✅
- [x] ShopifyProduct: storeId → marketplaceId
- [x] IShopifyColl: use Marketplace instead of IStore, implement IMarketplaceSync
- [x] IShopifyProductColl: update field references
- [x] IShopifyBusinessEntityColl: use Marketplace for ping()
- [x] IShopifyCollections: extend MarketPlazeCollections
- [x] Delete: IStore, IStoreColl, ICommonStore

## Phase 3: Tests ✅
- [x] MoneyTest: arithmetic, comparison, fromDecimal/toDecimal
- [x] PricingServiceTest: ProfitCalculator, MarketplaceComparator, PriceSuggester
- [x] ShopifyTest: @ShopifyField reflection test still passes

## Phase 4: Build & Publish ✅
- [x] ./gradlew build — compiles without errors
- [x] ./gradlew jvmTest — all tests pass
- [x] ./gradlew publishToMavenLocal — artifacts published as v2.0.0

## Phase 5: mercadoLibreLib Module ✅
- [x] MercadoLibreProduct model (StringId, title, categoryId, price, pictures, status)
- [x] CommonMercadoLibreProduct container
- [x] IMercadoLibreProductColl with abstract fetchProducts() + IMarketplaceSync
- [x] IMercadoLibreCollections factory extending MarketPlazeCollections

## Phase 6: amazonLib Module ✅
- [x] AmazonProduct model (StringId/ASIN, title, brand, category, fulfillmentChannel, images)
- [x] CommonAmazonProduct container
- [x] IAmazonProductColl with abstract fetchProducts() + IMarketplaceSync
- [x] IAmazonCollections factory extending MarketPlazeCollections

## Phase 7: KVision UI Components ✅
- [x] marketplacePanel — marketplace list table with selection
- [x] marketplaceCard — single marketplace card display
- [x] productPanel — product list with SKU, base cost, variants
- [x] costStructurePanel — commissions, fees, shipping tiers, modalities
- [x] costBreakdownPanel — profit/loss breakdown with color-coded net profit
- [x] comparisonPanel — cross-marketplace profit comparison table
- [x] competitorPricePanel — competitor price tracker with min/avg/max summary

## Phase 8: Sync Scheduler ✅
- [x] SyncScheduler class with coroutine-based task scheduling
- [x] schedule(), cancel(), cancelAll(), isRunning(), activeTaskIds(), shutdown()
- [x] Configurable interval per marketplace sync task

## Phase 9: Build & Publish All ✅
- [x] ./gradlew clean build — all 4 modules compile
- [x] ./gradlew jvmTest — 15 tests pass (10 Money + 4 Pricing + 1 Shopify)
- [x] ./gradlew publishToMavenLocal — all 4 modules published as v2.0.0
