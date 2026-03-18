# marketPlazeLib

A Kotlin Multiplatform library providing marketplace intelligence abstractions: multi-marketplace product management, cost structure modeling, profit calculation, competitive pricing, and sync interfaces.

**Version:** 1.2.0 | **Targets:** JVM · JavaScript (ES2015, IR)

---

## Modules

| Module | Description |
|--------|-------------|
| `commonLib` | Core domain models, pricing services, sync interface, KVision building blocks |
| `shopifyLib` | Shopify Admin API integration via Apollo GraphQL |
| `mercadoLibreLib` | MercadoLibre marketplace integration (abstract fetch pattern) |
| `amazonLib` | Amazon SP-API integration via ASIN-based products (abstract fetch pattern) |
| `samples` | Runnable sample app — Ktor + KVision, no MongoDB required |

---

## Quick Start

### Gradle (libs.versions.toml)

```toml
[versions]
marketPlazeLib = "1.2.0"

[libraries]
marketPlaze-common = { module = "com.fonrouge.marketPlazeLib:commonLib", version.ref = "marketPlazeLib" }
marketPlaze-shopify = { module = "com.fonrouge.marketPlazeLib:shopifyLib", version.ref = "marketPlazeLib" }
marketPlaze-mercadoLibre = { module = "com.fonrouge.marketPlazeLib:mercadoLibreLib", version.ref = "marketPlazeLib" }
marketPlaze-amazon = { module = "com.fonrouge.marketPlazeLib:amazonLib", version.ref = "marketPlazeLib" }
```

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.marketPlaze.common)
        }
        jvmMain.dependencies {
            implementation(libs.marketPlaze.shopify)
            implementation(libs.marketPlaze.mercadoLibre)
            implementation(libs.marketPlaze.amazon)
        }
    }
}
```

---

## Core Domain

### Money

`Money` is a `@JvmInline value class` wrapping `Long` cents — exact arithmetic, KMP-safe, no floating-point errors.

```kotlin
val price = Money(1999)      // $19.99
val tax   = Money(160)       // $1.60
val total = price + tax      // Money(2159)
```

### Key Entities

| Entity | Description |
|--------|-------------|
| `Marketplace` | A sales channel (Shopify, MercadoLibre, Amazon). Holds `connectionConfig`, tax rates (`ivaRate`, `isrRate`, `ivaIncluded`), dual commission rates (`commissionRateClassic`, `commissionRatePremium`), `cargoFijoTiers`, `envioTiers`, `envioDiscounts`, `minimumPrice`, `isDefault`. |
| `Product` | A sellable item with SKU, cost, `referralKitCost`, variants, images, presentations (with package dimensions), and ERP publishing fields. |
| `MarketplaceListing` | A `Product` listed on a `Marketplace` — tracks `externalId`, `price`, `discount`, `publicationType` (CLASICA/PREMIUM), `presentationIndex`, `cargoFijoOverride`, `envioOverride`. |
| `CostStructure` | Commission tiers, fixed fees, shipping tiers, advertising costs, and modalities per marketplace. |
| `CostBreakdown` | Calculated profit metrics: `finalPrice`, `taxableBase`, `ivaWithheld`, `isrWithheld`, `cargoFijo`, `costoEnvioLogistico`, `envioCliente`, `referralKitCost`, `margin`, `healthLevel`, per-piece metrics. |
| `CompetitorPrice` | Observed competitor pricing for a listing. |

### Pricing Services

```kotlin
// Calculate a full cost breakdown with Mexican marketplace cost modeling
val breakdown = ProfitCalculator.calculate(
    salePrice = Money(50000),       // $500 MXN
    baseCost = Money(20000),        // $200 MXN per piece
    costStructure = costStructure,
    marketplace = marketplace,      // holds dual commissions, cargo fijo, envío tiers
    presentation = presentation,    // piecesPerPackage, package dimensions
    publicationType = PublicationType.CLASICA,
    referralKitCost = Money(200),
    productWeight = 1.5,            // kg — used for envío logístico
)

// Compare profitability across multiple marketplaces
val result = MarketplaceComparator.compare(
    salePrice = salePrice,
    baseCost = baseCost,
    pairs = listOf(
        MarketplaceCostPair(shopify, shopifyCosts),
        MarketplaceCostPair(mercadoLibre, mlCosts),
    ),
)
val best = result.bestByProfit

// Compare presentations of a product (rank by profit per piece)
val comparison = PresentationComparator.compare(
    inputs = listOf(
        PresentationInput(individual, Money(15000)),
        PresentationInput(packX6, Money(70000)),
    ),
    baseCost = Money(5000),
    costStructure = costStructure,
    marketplace = marketplace,
)
val bestPresentation = comparison.bestByProfitPerPiece

// Suggest minimum price to achieve a target margin
val minPrice = PriceSuggester.minimumPriceForMargin(
    baseCost = Money(5000),
    costStructure = costStructure,
    marketplace = marketplace,
    targetMarginPercent = 20.0,
)

// Pre-configured marketplace defaults
val ml = MarketplaceDefaults.mercadoLibreMexico()   // 21 envío tiers, 4 cargo fijo tiers, 3 discounts
val amz = MarketplaceDefaults.amazonMexico()
```

### Shipping Calculator

```kotlin
// Volumetric weight: L×W×H / 5000, billable = max(real, volumetric)
val shippingCost = ShippingCalculator.calculateShippingCost(
    productWeight = 1.5,
    presentation = presentation,    // packageLength/Width/Height for volumetric
    salePrice = Money(50000),       // determines discount tier
    envioTiers = marketplace.envioTiers,
    envioDiscounts = marketplace.envioDiscounts,
)

// Cargo fijo resolution with override support
val cargoFijo = ShippingCalculator.resolveCargoFijo(
    finalPrice = Money(15000),
    cargoFijoTiers = marketplace.cargoFijoTiers,
    cargoFijoOverride = listing.cargoFijoOverride,
)
```

### Health Level

```kotlin
val health = HealthLevel.classify(marginPercent = 18.5)
// EXCELLENT (≥30%) / HEALTHY (≥15%) / ACCEPTABLE (≥10%) / CRITICAL (<10%)

// NO_DATA when cost is unknown
val noData = HealthLevel.classify(marginPercent = 50.0, baseCost = Money.ZERO)
// → NO_DATA
```

---

## Marketplace Integrations

### Shopify

Uses **Apollo GraphQL 4.3.3** against the Shopify Admin API (2025-10 schema).

- `@ShopifyField` annotation maps Kotlin properties to GraphQL node fields
- `IShopifyColl` — abstract MongoDB collection implementing `IMarketplaceSync`
- `IShopifyCollections` — abstract factory extending `MarketPlazeCollections`
- `ShopifyPushEngine` — implements `IShopifyPushService` (10 Apollo mutations: create/update/delete products, inventory, publish state, collections, media)
- `ShopifyBidirectionalSyncEngine` — orchestrates ERP→Shopify push via `reconcile()` + `syncToRemote()`
- Tests validate `@ShopifyField` mappings via reflection

```kotlin
// Pull sync (Shopify → local)
class MyShopifyProducts(db: MongoDatabase) : IShopifyProductColl(db) {
    override val marketplace: Marketplace get() = /* your marketplace */
}

// Push sync (ERP → Shopify)
val engine = ShopifyBidirectionalSyncEngine(
    erpReader = myErpReader,           // consumer implements IErpProductReader
    pushService = ShopifyPushEngine(),
    listingsProvider = { marketplace -> /* query MarketplaceListings */ },
    listingSaver = { listing -> /* save new MarketplaceListing */ },
)
val result = engine.syncToRemote(marketplace)
```

### MercadoLibre

Abstract fetch pattern — consumers provide the actual API client.

```kotlin
class MyMercadoLibreProducts(db: MongoDatabase) : IMercadoLibreProductColl(db) {
    override suspend fun fetchProducts(marketplace: Marketplace): List<MercadoLibreProduct> {
        // call MercadoLibre API here
    }
}
```

### Amazon

ASIN-based products. Abstract fetch pattern — consumers provide the SP-API client.

```kotlin
class MyAmazonProducts(db: MongoDatabase) : IAmazonProductColl(db) {
    override suspend fun fetchProducts(marketplace: Marketplace): List<AmazonProduct> {
        // call Amazon SP-API here
    }
}
```

---

## Sync Interface & Scheduler

All marketplace collections implement `IMarketplaceSync`:

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState
    suspend fun syncToRemote(marketplace: Marketplace): SimpleState
}
```

`SyncScheduler` is a coroutine-based JVM scheduler that runs sync tasks at configurable intervals:

```kotlin
val scheduler = SyncScheduler()
scheduler.schedule(interval = 30.minutes) {
    shopifyProducts.sync(marketplace)
    mercadoLibreProducts.sync(marketplace)
}
scheduler.start()
```

---

## Collections & Factory Pattern

Each entity follows the **entity triple** pattern:

| Part | Type | Role |
|------|------|------|
| Model | `BaseDoc` subclass | Data representation |
| Container | `data object` / `ICommonContainer` | Registry & metadata |
| Collection | `Coll` subclass (jvmMain) | MongoDB + business logic |

`MarketPlazeCollections` is the base abstract factory. Extend it to wire all collections:

```kotlin
class MyCollections(db: MongoDatabase) : IShopifyCollections(db) {
    override val products       = MyShopifyProducts(db)
    override val marketplaces   = MyMarketplaces(db)
    // ...
}
```

---

## KVision Building Blocks (JS)

The `commonLib` JS source set provides reusable KVision components:

- **Column definitions** (`colDef`) — typed table columns for each entity
- **Filter views** (`filter`) — off-canvas filter panels per entity
- **Form field builders** (`form`) — form inputs bound to entity fields

**View pattern** — list/detail screens use companion objects for service manager binding:

```kotlin
class ViewListProduct : ViewList<Product, ProductFilter>() {
    companion object {
        fun configViewList(app: Application) = ViewRegistry.register(...)
    }
}
```

---

## Typed Filters

Each entity has a concrete `IApiFilter` subclass with immutable `copy()`:

```kotlin
val filter = ProductFilter(sku = "ABC-123", active = true)
val updated = filter.copy(active = false)
```

Available filters: `MarketplaceFilter`, `ProductFilter` (sku, name, publishEnabled, status), `MarketplaceListingFilter`, `CostStructureFilter`, `CostBreakdownFilter`, `CompetitorPriceFilter`, `ShopifyProductFilter`, `ShopifyCollectionFilter`.

---

## Sample Application

A runnable full-stack demo (no MongoDB required):

```bash
./gradlew :samples:run
# Opens Ktor server at http://localhost:8080
```

The sample uses `fsLib`'s `InMemoryRepository` and seeds:
- 3 Marketplaces (Shopify, MercadoLibre, Amazon)
- 3 Products with variants and presentations (with package dimensions)
- 6 Marketplace listings (with publicationType and presentationIndex)
- Cost structures, breakdowns (with envío logístico), and competitor prices

Frontend: KVision app with 7 list views and 3 item detail views.

---

## Build & Test

```bash
./gradlew build                   # Build all modules
./gradlew test                    # Run all tests
./gradlew jvmTest                 # Run JVM tests only
./gradlew :commonLib:jvmTest      # Money + pricing service tests
./gradlew :shopifyLib:jvmTest     # Shopify annotation tests
./gradlew publishToMavenLocal     # Publish to local Maven repo
```

The test suite covers `Money` arithmetic, `ProfitCalculator` (IVA/ISR withholding, dual commissions, cargo fijo, envío logístico, cost×pieces scaling, referralKitCost, envioCliente, NO_DATA health), `ShippingCalculator` (volumetric weight, tier resolution, overrides), `PresentationComparator`, `MarketplaceDefaults`, `MarketplaceComparator`, `PriceSuggester`, `SyncScheduler`, Shopify `@ShopifyField` annotation mappings, `Product` enrichment serialization, and `ShopifyBidirectionalSyncEngine` reconcile/push logic.

---

## Key Dependencies

| Library | Version | Role |
|---------|---------|------|
| Kotlin Multiplatform | 2.3.10 | Language & targets |
| fsLib | 3.1.2-SNAPSHOT | BaseDoc, Coll, InMemoryRepository, ViewList/ViewItem |
| KVision | 9.3.1 | UI framework (JS target) |
| Kilua RPC | 0.0.42 | Type-safe RPC for KMP |
| Apollo GraphQL | 4.3.3 | Shopify Admin API client |
| kotlinx-serialization | 1.10.0 | Serialization |
| kotlinx-datetime | 0.6.1 | Date/time |

---

## Architecture Notes

- **Money as cents**: All monetary values use `Money(Long)` — never `Double` or `BigDecimal`.
- **Marketplace credentials**: Stored as `Map<String, String>` in `Marketplace.connectionConfig` — keyed by marketplace type.
- **Mexican marketplace pricing (v1.2.0)**: Full cost modeling for MercadoLibre and Amazon Mexico — dual commission rates (clásica/premium), cargo fijo tiers, envío logístico with volumetric weight, price-based shipping discounts, referralKitCost, envioCliente, per-piece cost scaling, and `HealthLevel.NO_DATA`. `MarketplaceDefaults` provides ready-to-use configurations. `PresentationComparator` ranks presentations by profit per piece.
- **Mexican tax support**: `ProfitCalculator` handles IVA withholding (8%) and ISR withholding (2.5%) for MercadoLibre Mexico (`ivaIncluded = true`).
- **Bidirectional sync (v1.1.0)**: `ShopifyPushEngine` + `ShopifyBidirectionalSyncEngine` enable ERP→Shopify product publishing. Consumers implement `IErpProductReader`; the engine handles reconciliation, creation, updates, inventory, media, and collection management.
- **Multiplatform safety**: All core domain logic lives in `commonMain`; MongoDB and coroutine-heavy code is in `jvmMain`.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## License

Copyright © Fonrouge. All rights reserved.
