# Changelog

All notable changes to this project will be documented in this file.

## [2.0.1] - 2026-03-14

### Changed
- Upgrade to fsLib 3.0.4-SNAPSHOT — adapt to breaking API changes:
  - `Coll<T, ID, FILT, UID>` (removed CC type parameter)
  - `ICommonContainer` now takes `filterKClass: KClass<FILT>` instead of `idSerializer` + `apiFilterSerializer`
  - `ViewList<T, ID, FILT, MID>` and `ViewItem<T, ID, FILT>` (removed CC type parameter)
  - `InMemoryRepository<T, ID, FILT, UID>` (removed CC type parameter)
  - `IShopifyColl` reduced from 7 to 6 type parameters
- **samples**: Runnable sample app following fsLib showcase pattern:
  - Uses `InMemoryRepository` (memorydb) — no MongoDB required
  - `copyJsBundleToAssets` Gradle task for JS→JVM asset pipeline
  - `ViewRegistry` + `KVWebManager.initialize` for proper view routing
  - Companion object `configViewList`/`configViewItem` per ViewList/ViewItem class
  - Ktor server with seed data on port 8080
  - Run with `./gradlew :samples:run`
- **mercadoLibreLib/amazonLib**: Added concrete filter classes (`MercadoLibreProductFilter`, `AmazonProductFilter`), converted CommonContainers to data objects
- Filter view extension functions capture `viewList` reference explicitly for nested lambda access

## [2.0.0] - 2026-03-14

### Added
- **commonLib**: 6 new entities with BaseDoc + ICommonContainer (data object) + Coll triple:
  - `Marketplace` — registered marketplace with generic `connectionConfig: Map<String, String>`
  - `Product` — own product with SKU, base cost, variants, images
  - `MarketplaceListing` — product published on a marketplace
  - `CostStructure` — commissions, fees, shipping tiers, advertising, modalities per marketplace
  - `CostBreakdown` — calculated cost breakdown per product x marketplace
  - `CompetitorPrice` — observed competitor price
- **commonLib**: `Money` value class (`Long` cents) with full arithmetic operators
- **commonLib**: 9 value types: `Commission`, `Fee`, `ShippingTier`, `AdvertisingRate`, `PublicationModality`, `FeeLineItem`, `ProductVariant`, `ProductImage`
- **commonLib**: 6 typed filter classes: `MarketplaceFilter`, `ProductFilter`, `MarketplaceListingFilter`, `CostStructureFilter`, `CostBreakdownFilter`, `CompetitorPriceFilter`
- **commonLib**: Pricing services (pure, KMP-safe):
  - `ProfitCalculator` — salePrice + baseCost + CostStructure → CostBreakdown
  - `MarketplaceComparator` — cross-marketplace profit ranking
  - `PriceSuggester` — minimum price for target margin, competitive pricing strategies
- **commonLib**: `IMarketplaceSync<T>` interface for marketplace sync abstraction
- **commonLib**: `SyncScheduler` — coroutine-based scheduler for periodic sync tasks
- **commonLib**: `MarketPlazeCollections` abstract factory for all entity collections
- **commonLib**: KVision view building blocks (jsMain):
  - 21 reusable column definitions with Tabulator headerFilter/headerSort/formatters
  - 3 off-canvas filter view builders (Marketplace, MarketplaceListing, CompetitorPrice)
  - 3 form field builders (Marketplace, Product, CompetitorPrice)
- **shopifyLib**: `ShopifyProductFilter` with `masterItemId: MarketplaceId?`
- **shopifyLib**: `IShopifyColl` now implements `IMarketplaceSync<T>`
- **mercadoLibreLib**: New module — `MercadoLibreProduct` model, `IMercadoLibreProductColl` with abstract `fetchProducts()` implementing `IMarketplaceSync`
- **amazonLib**: New module — `AmazonProduct` model (ASIN-based), `IAmazonProductColl` with abstract `fetchProducts()` implementing `IMarketplaceSync`
- **samples**: New module — complete consuming app showcase:
  - RPC service interfaces (`ISampleListService`, `ISampleItemService`)
  - 8 concrete collection classes + `SampleCollections` factory
  - 10 ConfigView registrations
  - 10 ViewList/ViewItem classes composing library building blocks
  - 14 integration tests (models, filters, pricing services, end-to-end scenario)
- **blueprints**: Architecture documentation (BRIEF, SPECIFICATION, FLOWCHART, API_CONTRACT, VIEW_MAP, IMPLEMENTATION_PLAN, TEST_PLAN, TRACEABILITY_MATRIX)
- 29 tests total across commonLib (10 Money + 4 Pricing), shopifyLib (1 ShopifyField), samples (14 integration)

### Changed
- **commonLib**: CommonContainers converted from `abstract class<FILT>` to concrete `data object` singletons
- **commonLib**: Coll classes simplified from 3 generic params (`CC, FILT, UID`) to 1 (`UID`)
- **shopifyLib**: `ShopifyProduct.storeId` → `marketplaceId: MarketplaceId`
- **shopifyLib**: `ICommonShopifyProduct` → `CommonShopifyProduct` (data object)
- **shopifyLib**: `IShopifyColl.buildApolloClient()` now takes `Marketplace` instead of `IStoreId`
- **shopifyLib**: `IShopifyCollections` extends `MarketPlazeCollections`, removed `StoreColl`
- **shopifyLib**: `IShopifyProductColl` simplified to single `<UID>` generic
- Version bump to 2.0.0
- fsLib dependency updated to 3.0.3

### Removed
- `MarketPlaceId` enum — replaced by `Marketplace.type: String`
- `IMarketProductId<PID>` — replaced by `MarketplaceListing.externalId`
- `IMarketProduct<ID>` — replaced by `Product` + `MarketplaceListing`
- `IMarketProductColl` — replaced by `ProductColl` + `MarketplaceListingColl`
- `IStore` / `IStoreId` — replaced by `Marketplace` with `connectionConfig`
- `IStoreColl` — replaced by `MarketplaceColl`
- `ICommonStore` — replaced by `CommonMarketplace`

## [1.0.2] - 2024-11-08

### Changed
- Migrate to fsLib 3.0.0

## [1.0.1] - 2024-11-08

### Changed
- Update fsLib to 1.9.0
