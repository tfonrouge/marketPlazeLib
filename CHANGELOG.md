# Changelog

All notable changes to this project will be documented in this file.

> **Note on version history**: Versions 1.1.0 through 1.3.0 were published after 2.1.0 due to a version numbering error.
> Starting from 2.2.0, versioning continues correctly from the 2.x lineage.

## [Unreleased]

## [2.7.0] - 2026-06-24

### Changed
- **Toolchain migration to the fsLib 6.0.0 ecosystem (requires JDK 25).** Driven by mppErsaPack's migration to fsLib 6.0.0, whose public `inline` functions are compiled at JVM target 25. Raising the JVM toolchain to **25** (JVM target 25) across all modules; published JVM bytecode is now major version 69 (Java 25, was 65 / Java 21). Consumers must build on JDK 25.
- **Eliminates the stale fsLib 3.2.0 transitive request.** Until now mppErsaPack only linked this library by relying on Gradle's silent `3.2.0 -> 6.0.0` conflict-resolution upgrade — a latent runtime/hygiene exposure. The published 2.7.0 POMs now request fsLib 6.0.0 directly. `allTests` is green on the new stack (77 JVM tests), and the downstream mppErsaPack build gate was re-verified green with no remaining version-upgrade arrows.

### Dependencies
- fsLib 3.2.0 -> **6.0.0**
- Kotlin 2.3.20 -> **2.4.0**; KSP 2.3.6 -> **2.3.9**
- KVision 9.5.0 -> **9.6.0**; kilua-rpc 0.0.43 -> **0.0.45**
- Apollo 4.4.2 -> **5.0.0** (4.4.x's Gradle plugin references `KotlinJsTarget`, removed in Kotlin 2.4.0)
- kotlinx-serialization 1.10.0 -> **1.11.0**
- Removed dead `dulceLib` catalog entry (referenced by nothing)

## [2.5.0] - 2026-05-01

### Added
- **`MarketplaceType` constants object** in `commonLib` — string constants `MERCADO_LIBRE`, `SHOPIFY`, `AMAZON` to replace magic strings when populating `Marketplace.type`

### Changed
- **Build: centralize all versions in catalog** — Apollo (4.3.3 -> 4.4.2), Dokka (2.0.0 -> 2.2.0), Ktor (3.4.0 -> 3.4.2) moved to `libs.versions.toml`
- **Build: Dokka V1 -> V2** — migrated from deprecated Dokka V1 plugin to V2 (version 2.2.0)
- **Build: remove redundant dependencies** — mercadoLibreLib, amazonLib, shopifyLib, and samples no longer redeclare transitive deps from commonLib; commonLib now exposes core deps as `api` instead of `implementation`
- **Shopify: replace deprecated GraphQL fields** — `productCreateMedia` -> `productUpdate` with media param; `productDeleteMedia` -> `fileDelete`

## [2.4.0] - 2026-04-01

### Added
- `getProduct()` query in `IShopifyPushService` + `ShopifyPushEngine` — fetches product by Shopify GID
- `ShopifySmokeTest` round-trip integration test

### Dependencies
- Kotlin 2.3.20, KSP 2.3.6, kilua-rpc 0.0.43

## [2.3.0] - 2026-03-31

### Added
- **Shopify smart collection CRUD**: `createCollection`, `updateCollection`, `deleteCollection` in `IShopifyPushService` + `ShopifyPushEngine` via Apollo GraphQL `CollectionCreate`, `CollectionUpdate`, `CollectionDelete` mutations
- **`CollectionRule`** data class (`column`, `relation`, `condition`) matching Shopify's `CollectionRuleInput` schema
- **`ShopifyCollection`** expanded with `rules: List<CollectionRule>`, `isSmartCollection: Boolean`, `image: String?`
- **Binary image upload**: `uploadImageFromBytes()` in `IShopifyPushService` — 3-step flow: `stagedUploadsCreate` -> HTTP multipart PUT -> `productCreateMedia`
- **Image processing pipeline** (JVM-only, no external deps): `ImageProcessor.pipeline().resize(w,h).watermark(text,pos,opacity).toJpeg(quality).process(bytes)` using `java.awt`
- **`ImageHasher`**: SHA-256 deduplication via `java.security.MessageDigest`
- **`ImageSize`** enum: `BASE(470)`, `SMALL(320)`, `THUMBNAIL(100)`
- Image utilities: `ByteArray.isValidImage()` (magic bytes), `ByteArray.imageDimensions()`
- **Field-level diff engine**: `detectChanges()` enhanced from 2 fields to 10+: title, descriptionHtml, price, barcode, vendor, weight, tags, published, inventoryQuantity, productType
- **`FieldChange`** data class (`fieldName`, `oldValue`, `newValue`) for rich diff reporting
- **`BulkSyncResult`** data class (`created`, `updated`, `failed`, `errors`) for batch operation reporting
- **`bulkSyncToRemote()`** in `ShopifyBidirectionalSyncEngine`: batched create/update with configurable batch size and delay between API calls
- **`Product`** expanded with `vendor: String?`, `productType: String?`
- **`MarketplaceListing`** expanded with 7 diff-tracking fields: `descriptionHtml`, `barcode`, `vendor`, `weight`, `tags`, `inventoryQuantity`, `productType`
- **`IErpProductReader`**: `readInventoryByLocation(): Map<String, Map<String, Int>>` for per-location inventory sync (default empty map, backward compatible)
- Richer listing persistence in `syncToRemote()` — saves all expanded fields for accurate next-reconcile comparison

### Changed
- **BREAKING**: `SyncDiff.changedFields` type changed from `List<String>` to `List<FieldChange>`. Migration: replace `.contains("title")` with `.any { it.fieldName == "title" }`

### Dependencies
- fsLib 3.2.0, KVision 9.5.0 (all stable releases, no SNAPSHOTs)

## [2.2.0] - 2026-03-31

### Changed
- **Version fix**: resumed correct 2.x versioning from 2.1.0 lineage (versions 1.1.0-1.3.0 were misnumbered)
- Upgrade fsLib from 3.2.0-SNAPSHOT to 3.2.0 (stable)
- Upgrade KVision from 9.4.0-SNAPSHOT to 9.5.0 (stable)
- Samples module now uses catalog version instead of hardcoded 1.0.0-SNAPSHOT
- FullShopifySync(FEATURE) blueprint added

## [1.3.0] - 2026-03-27

### Changed
- Upgrade fsLib to 3.2.0-SNAPSHOT, KVision to 9.4.0-SNAPSHOT

## [1.2.0] - 2026-03-18

### Added
- **Mexican marketplace pricing engine** imported from MarketPlaceCalc blueprint
- `Marketplace` model: `commissionRateClassic`, `commissionRatePremium` (dual commission rates), `cargoFijoTiers`, `envioTiers`, `envioDiscounts`, `isDefault`, `minimumPrice`
- `Product` model: `referralKitCost: Money`
- `MarketplaceListing` model: `presentationIndex`, `publicationType` (CLASICA/PREMIUM), `cargoFijoOverride`, `envioOverride`
- `CostBreakdown` model: `cargoFijo`, `costoEnvioLogistico`, `envioCliente`, `referralKitCost`
- `ProductPresentation`: `packageWeight`, `packageLength`, `packageWidth`, `packageHeight` for volumetric weight
- `CargoFijoTier`, `EnvioTier`, `EnvioDiscount` value types for tier-based cost resolution
- `PublicationType` enum (CLASICA, PREMIUM)
- `HealthLevel.NO_DATA` — returned when `baseCost <= 0`
- `ShippingCalculator` service: volumetric weight (`L*W*H/5000`), weight-tier resolution, price-based discounts, override support, `resolveCargoFijo()`
- `PresentationComparator` service: ranks presentations by `profitPerPiece`
- `MarketplaceDefaults` service: pre-configured MercadoLibre Mexico (21 envio tiers, 3 discounts, 4 cargo fijo tiers) and Amazon Mexico
- 17 new tests

### Changed
- **ProfitCalculator**: baseCost now scaled by `totalPieces`; dual commission rates from Marketplace take precedence over CostStructure commissions
- **HealthLevel.classify()**: new optional `baseCost` parameter — returns `NO_DATA` when cost is zero/negative

## [1.1.0] - 2026-03-18

### Added
- **ShopifyPushSync bridge** — bidirectional Shopify sync engine (`ShopifyBidirectionalSyncEngine`)
- `IShopifyPushService` interface: `createProduct`, `updateProduct`, `deleteProduct`, `updateInventory`, `setPublishState`, `addToCollection`, `removeFromCollection`, `uploadImage`, `deleteImage`, `reorderImages`
- `ShopifyPushEngine` implementation via Apollo GraphQL
- `IErpProductReader` interface for ERP product sourcing
- `SyncDiff` + `detectChanges()` for reconciliation
- MercadoLibreOrders(FEATURE) blueprint and `MercadoLibreOrder` model

## [2.1.0] - 2026-03-15

### Added
- **Mexican marketplace pricing engine**: IVA withholding (8%), ISR withholding (2.5%), taxable base calculation, discount support, shipping tier resolution, profit health classification, per-piece metrics
- `Marketplace` model: `ivaRate`, `isrRate`, `ivaIncluded` fields
- `MarketplaceListing` model: `discount` (0.0-1.0) and `listingUrl` fields
- `CostBreakdown` model: `finalPrice`, `taxableBase`, `ivaWithheld`, `isrWithheld`, `shippingCost`, `healthLevel`, `piecesCount`, `pricePerPiece`, `costPerPiece`, `profitPerPiece`
- `ProductPresentation`, `PriceListEntry`, `HealthLevel` types
- 6 new pricing tests (43 total)

### Changed
- **BREAKING**: `ProfitCalculator.calculate()` — new params: `marketplace`, `discount`, `presentation`, `shippingCostOverride`
- **BREAKING**: `MarketplaceComparator.compare()` — `costStructures` param replaced by `pairs: List<MarketplaceCostPair>`
- **BREAKING**: `PriceSuggester.minimumPriceForMargin()` — new `marketplace` param
- **BREAKING**: `ShippingTier` — `minWeight`/`maxWeight` renamed to `minValue`/`maxValue`, added `tierType`

### Removed
- `MarketplacePricing(BRIDGE)` directory — absorbed into MODULE

## [2.0.2] - 2026-03-15

### Changed
- Upgrade fsLib from 3.0.4-SNAPSHOT to 3.1.1 (stable release)

## [2.0.1] - 2026-03-15

### Added
- 8 `SyncScheduler` tests (37 total)
- `@RpcBindingRoute` annotations on all 14 sample service interface methods

### Changed
- Adapt to fsLib 3.0.4+ breaking API changes
- **samples**: Runnable sample app following fsLib showcase pattern
- **mercadoLibreLib/amazonLib**: Added concrete filter classes, converted CommonContainers to data objects

### Removed
- `imageBanner.kt` — dead relic

## [2.0.0] - 2026-03-14

### Added
- **commonLib**: 6 new entities (Marketplace, Product, MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice)
- **commonLib**: `Money` value class, 9 value types, 6 typed filters
- **commonLib**: Pricing services: `ProfitCalculator`, `MarketplaceComparator`, `PriceSuggester`
- **commonLib**: `IMarketplaceSync<T>`, `SyncScheduler`, `MarketPlazeCollections` factory
- **commonLib**: KVision view building blocks (21 column defs, 3 filter views, 3 form builders)
- **shopifyLib**: `IShopifyColl` implements `IMarketplaceSync<T>`
- **mercadoLibreLib**: New module
- **amazonLib**: New module
- **samples**: New module — complete consuming app showcase
- 29 tests total

### Changed
- **BREAKING**: Complete entity model redesign from 1.x
- fsLib dependency updated to 3.0.3

### Removed
- `MarketPlaceId` enum, `IMarketProductId`, `IMarketProduct`, `IMarketProductColl`, `IStore`/`IStoreId`/`IStoreColl`, `ICommonStore`

## [1.0.2] - 2024-11-08

### Changed
- Migrate to fsLib 3.0.0

## [1.0.1] - 2024-11-08

### Changed
- Update fsLib to 1.9.0
