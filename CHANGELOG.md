# Changelog

All notable changes to this project will be documented in this file.

## [2.1.0] - 2026-03-15

### Added
- **Mexican marketplace pricing engine**: IVA withholding (8%), ISR withholding (2.5%), taxable base calculation (`finalPrice / 1.16`), discount support, shipping tier resolution, profit health classification (Critical/Acceptable/Healthy/Excellent), per-piece metrics
- `Marketplace` model: `ivaRate`, `isrRate`, `ivaIncluded` fields for tax withholding configuration
- `MarketplaceListing` model: `discount` (0.0-1.0) and `listingUrl` fields
- `CostBreakdown` model: `finalPrice`, `taxableBase`, `ivaWithheld`, `isrWithheld`, `shippingCost`, `healthLevel`, `piecesCount`, `pricePerPiece`, `costPerPiece`, `profitPerPiece`
- `ProductPresentation` value type (label, piecesPerPackage, packagesPerDisplay, totalPieces, packagingCost, isHypothetical)
- `PriceListEntry` value type (name, price, currencyCode, compareAtPrice)
- `HealthLevel` enum with `classify(marginPercent)` companion method
- `resolveShippingCost(price, tiers)` — price-based shipping tier resolution function
- `Product` model: `presentations: List<ProductPresentation>`
- 6 new pricing tests: MX taxes, discount, presentation per-piece, shipping resolution, health classification, PriceSuggester with taxes (43 total tests)
- `ShopifyPushSync(BRIDGE)` blueprint for future ERP→Shopify push capability
- `scripts/render_flowchart.py` for rendering Mermaid diagrams to HTML

### Changed
- **BREAKING**: `ProfitCalculator.calculate()` — new params: `marketplace`, `discount`, `presentation`, `shippingCostOverride`; commissions now applied to `finalPrice` (after discount) instead of `salePrice`
- **BREAKING**: `MarketplaceComparator.compare()` — `costStructures` param replaced by `pairs: List<MarketplaceCostPair>` (marketplace + cost structure together)
- **BREAKING**: `PriceSuggester.minimumPriceForMargin()` — new `marketplace` param; accounts for IVA/ISR in denominator
- **BREAKING**: `ShippingTier` — `minWeight`/`maxWeight` renamed to `minValue`/`maxValue`, added `tierType` ("price"/"weight")
- Absorbed `MarketplacePricing(BRIDGE)` blueprint into `MarketPlazeLib(MODULE)` as Phase 8
- Updated all MODULE blueprint artifacts (SPECIFICATION, API_CONTRACT, FLOWCHART, TEST_PLAN, TRACEABILITY_MATRIX) to reflect current state

### Removed
- `MarketplacePricing(BRIDGE)` directory — work absorbed into MODULE

## [2.0.2] - 2026-03-15

### Changed
- Upgrade fsLib from 3.0.4-SNAPSHOT to 3.1.1 (stable release)

## [2.0.1] - 2026-03-15

### Added
- 8 `SyncScheduler` tests: schedule, cancel, cancelAll, isRunning, onResult callback, failure resilience, reschedule (37 total tests now)
- `@RpcBindingRoute` annotations on all 14 sample service interface methods for proper Kilua RPC route registration

### Changed
- Adapt to fsLib 3.0.4+ breaking API changes: `Coll`, `ViewList`, `ViewItem`, `InMemoryRepository` all dropped `CC` type parameter; `ICommonContainer` uses `filterKClass` instead of `idSerializer`/`apiFilterSerializer`; `IShopifyColl` reduced from 7 to 6 type parameters
- **samples**: Runnable sample app following fsLib showcase pattern — `InMemoryRepository` (no MongoDB), `copyJsBundleToAssets` Gradle task, `ViewRegistry` + `KVWebManager.initialize`, companion object ConfigView, Ktor server with seed data on port 8080
- **mercadoLibreLib/amazonLib**: Added concrete filter classes (`MercadoLibreProductFilter`, `AmazonProductFilter`), converted CommonContainers to data objects
- Consolidated 6 individual CommonContainer files into single `CommonContainers.kt`
- Renamed `ICommonShopifyProduct.kt` → `CommonShopifyProduct.kt` to match class name
- Form builders now use `formRow`/`formColumn` layout from fsLib
- Filter view extension functions capture `viewList` reference explicitly for nested lambda access
- Updated `CLAUDE.md`: data object containers, samples module, typed filters, view pattern docs

### Removed
- `imageBanner.kt` — dead relic with hardcoded project-specific images

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
