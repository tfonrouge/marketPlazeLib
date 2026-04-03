# marketPlazeLib

A Kotlin Multiplatform library for multi-marketplace e-commerce: product management, cost modeling, profit calculation, competitive pricing, bidirectional sync, and image processing.

**Version:** 2.4.0 | **Targets:** JVM + JavaScript (ES2015, IR) | **Published to:** Maven Local

---

## Table of Contents

- [Modules](#modules)
- [Installation](#installation)
- [Core Domain](#core-domain)
  - [Money](#money)
  - [Entities](#entities)
  - [Pricing Services](#pricing-services)
  - [Shipping Calculator](#shipping-calculator)
  - [Health Level](#health-level)
  - [Image Processing (JVM)](#image-processing-jvm)
- [Marketplace Integrations](#marketplace-integrations)
  - [Shopify](#shopify)
  - [MercadoLibre](#mercadolibre)
  - [Amazon](#amazon)
- [Sync Engine](#sync-engine)
  - [Pull Sync (Marketplace -> Local)](#pull-sync-marketplace---local)
  - [Push Sync (ERP -> Shopify)](#push-sync-erp---shopify)
  - [Bulk Operations](#bulk-operations)
  - [Scheduler](#scheduler)
  - [Smoke Testing](#smoke-testing)
- [Collections & Factory Pattern](#collections--factory-pattern)
- [KVision Building Blocks (JS)](#kvision-building-blocks-js)
- [Typed Filters](#typed-filters)
- [Sample Application](#sample-application)
- [Build & Test](#build--test)
- [Dependencies](#dependencies)
- [Architecture Notes](#architecture-notes)

---

## Modules

| Module | Description |
|--------|-------------|
| `commonLib` | Core domain models, pricing services, image processing, sync interfaces, KVision building blocks |
| `shopifyLib` | Shopify Admin API integration via Apollo GraphQL — bidirectional sync, collection CRUD, image pipeline |
| `mercadoLibreLib` | MercadoLibre integration — products and orders (abstract fetch pattern) |
| `amazonLib` | Amazon SP-API integration — ASIN-based products (abstract fetch pattern) |
| `samples` | Runnable full-stack demo app — Ktor + KVision, no MongoDB required |

---

## Installation

**Prerequisites:** JDK 21+, Gradle 9.x

### 1. Publish to local Maven (from this repo)

```bash
./gradlew publishToMavenLocal
```

### 2. Consume in your project

**gradle/libs.versions.toml:**

```toml
[versions]
marketPlazeLib = "2.4.0"

[libraries]
marketPlaze-common = { module = "com.fonrouge.marketPlazeLib:commonLib", version.ref = "marketPlazeLib" }
marketPlaze-shopify = { module = "com.fonrouge.marketPlazeLib:shopifyLib", version.ref = "marketPlazeLib" }
marketPlaze-mercadoLibre = { module = "com.fonrouge.marketPlazeLib:mercadoLibreLib", version.ref = "marketPlazeLib" }
marketPlaze-amazon = { module = "com.fonrouge.marketPlazeLib:amazonLib", version.ref = "marketPlazeLib" }
```

**build.gradle.kts:**

```kotlin
repositories {
    mavenCentral()
    mavenLocal()   // required — not published to Maven Central
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.marketPlaze.common)     // always needed
        }
        jvmMain.dependencies {
            implementation(libs.marketPlaze.shopify)      // if using Shopify
            implementation(libs.marketPlaze.mercadoLibre) // if using MercadoLibre
            implementation(libs.marketPlaze.amazon)       // if using Amazon
        }
    }
}
```

`commonLib` transitively provides: fsLib (BaseDoc, Coll, ViewList), kotlinx-serialization, kotlinx-datetime, KVision common-remote. Marketplace modules transitively provide `commonLib`.

---

## Core Domain

### Money

`Money` is a `@JvmInline value class` wrapping `Long` cents. Exact arithmetic, KMP-safe, no floating-point errors.

```kotlin
val price = Money(1999)            // $19.99
val tax   = Money(160)             // $1.60
val total = price + tax            // Money(2159)
val perUnit = total / 3            // Money(719)
val doubled = price * 2            // Money(3998)

Money.ZERO                         // Money(0)
Money.fromDecimal(19, 2)           // Money(1900) — 19.00
price.toDecimal()                  // 19.99
```

### Entities

Every entity follows the **entity triple** pattern: model (`BaseDoc`) + container (`data object`) + collection (`Coll` subclass in jvmMain).

| Entity | ID type | Description |
|--------|---------|-------------|
| `Marketplace` | `OId` | A sales channel — credentials in `connectionConfig`, tax rates, dual commissions, cargo fijo/envio tiers |
| `Product` | `OId` | Sellable item — SKU, cost, variants, images, presentations (with package dimensions), ERP fields |
| `MarketplaceListing` | `OId` | Product published on a marketplace — price, discount, publicationType, overrides, 10+ tracking fields |
| `CostStructure` | `OId` | Commission tiers, fixed fees, shipping tiers, advertising costs per marketplace |
| `CostBreakdown` | `OId` | Calculated metrics: finalPrice, taxes withheld, shipping, margin, healthLevel, per-piece metrics |
| `CompetitorPrice` | `OId` | Observed competitor pricing for a listing |
| `ShopifyProduct` | `StringId` | Shopify product with media, vendor, handle |
| `ShopifyCollection` | `StringId` | Smart collection with rules, handle |
| `MercadoLibreProduct` | `StringId` | ML product with category, pictures, sellerSku |
| `MercadoLibreOrder` | `StringId` | ML order with buyer, items, shipping, payments |
| `AmazonProduct` | `StringId` | Amazon product with ASIN, bulletPoints, fulfillmentChannel |

### Pricing Services

All pricing services are pure functions in `commonMain` — no database, no IO.

#### ProfitCalculator

Computes a full `CostBreakdown` from sale price + cost inputs:

```kotlin
val breakdown = ProfitCalculator.calculate(
    salePrice = Money(50000),            // $500 MXN
    baseCost = Money(20000),             // $200 MXN per piece
    costStructure = costStructure,
    marketplace = marketplace,           // dual commissions, cargo fijo, envio tiers
    presentation = presentation,         // piecesPerPackage, package dimensions
    publicationType = PublicationType.CLASICA,
    referralKitCost = Money(200),
    productWeight = 1.5,                 // kg — for envio logistico
)

breakdown.netProfit          // Money
breakdown.profitMarginPercent // Double
breakdown.healthLevel        // "HEALTHY"
breakdown.profitPerPiece     // Money? (if presentation provided)
breakdown.cargoFijo          // Money
breakdown.costoEnvioLogistico // Money
breakdown.ivaWithheld        // Money (8% for ML Mexico)
breakdown.isrWithheld        // Money (2.5% for ML Mexico)
```

#### MarketplaceComparator

Ranks profitability across marketplaces:

```kotlin
val result = MarketplaceComparator.compare(
    salePrice = Money(50000),
    baseCost = Money(20000),
    pairs = listOf(
        MarketplaceCostPair(shopify, shopifyCosts),
        MarketplaceCostPair(mercadoLibre, mlCosts),
    ),
)
result.bestByProfit   // CostBreakdown with highest net profit
result.bestByMargin   // CostBreakdown with highest margin %
```

#### PresentationComparator

Compares product presentations (individual, 6-pack, display) by profit per piece:

```kotlin
val comparison = PresentationComparator.compare(
    inputs = listOf(
        PresentationInput(individual, Money(15000)),
        PresentationInput(packX6, Money(70000)),
    ),
    baseCost = Money(5000),
    costStructure = costStructure,
    marketplace = marketplace,
)
comparison.bestByProfitPerPiece  // ProductPresentation
```

#### PriceSuggester

```kotlin
// Minimum price for a target margin
val minPrice = PriceSuggester.minimumPriceForMargin(
    baseCost = Money(5000),
    costStructure = costStructure,
    marketplace = marketplace,
    targetMarginPercent = 20.0,
)

// Competitive pricing from observed competitor data
val price = PriceSuggester.competitivePrice(
    competitorPrices = competitors,
    strategy = PriceStrategy.BELOW_AVERAGE,
)
```

#### MarketplaceDefaults

Pre-configured marketplace templates:

```kotlin
val ml = MarketplaceDefaults.mercadoLibreMexico()
// 21 envio tiers, 4 cargo fijo tiers, 3 discounts, IVA 8%, ISR 2.5%

val amz = MarketplaceDefaults.amazonMexico()
```

### Shipping Calculator

```kotlin
// Volumetric weight: L x W x H / 5000, billable = max(real, volumetric)
val shippingCost = ShippingCalculator.calculateShippingCost(
    productWeight = 1.5,
    presentation = presentation,        // packageLength/Width/Height
    salePrice = Money(50000),           // determines discount tier
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
// EXCELLENT (>=30%) | HEALTHY (>=15%) | ACCEPTABLE (>=10%) | CRITICAL (<10%)

// NO_DATA when cost is unknown
val noData = HealthLevel.classify(marginPercent = 50.0, baseCost = Money.ZERO)
// -> NO_DATA
```

### Image Processing (JVM)

Server-side image processing pipeline using `java.awt` (no external dependencies):

```kotlin
// Fluent builder API
val processed = ImageProcessor.pipeline()
    .resize(ImageSize.BASE)                                  // max 470px
    .watermark("My Brand", WatermarkPosition.BOTTOM_RIGHT)   // semi-transparent
    .toJpeg(0.85f)                                           // quality 85%
    .process(imageBytes)                                     // ByteArray -> ByteArray

// Deduplication
val hash = ImageHasher.sha256(imageBytes)

// Validation
imageBytes.isValidImage()     // checks magic bytes (JPEG, PNG, WebP, GIF)
imageBytes.imageDimensions()  // Pair<Int, Int> (width, height)
```

---

## Marketplace Integrations

### Shopify

Uses **Apollo GraphQL 4.4.2** against the Shopify Admin API (2025-10 schema).

#### Key components

| Class | Role |
|-------|------|
| `ShopifyPushEngine` | 15 GraphQL mutations/queries: product CRUD, inventory, publish, collections, media, getProduct |
| `ShopifyBidirectionalSyncEngine` | Orchestrates ERP -> Shopify reconciliation and push |
| `IShopifyColl` | Abstract MongoDB collection implementing `IMarketplaceSync` for pull sync |
| `IShopifyCollections` | Abstract factory extending `MarketPlazeCollections` |
| `@ShopifyField` | Annotation mapping Kotlin properties to GraphQL node fields |

#### Pull sync (Shopify -> local MongoDB)

```kotlin
class MyShopifyProducts(db: MongoDatabase) : IShopifyProductColl<ObjectId>(db) {
    override val marketplace: Marketplace get() = /* your marketplace config */
}

// Syncs all products from Shopify to local MongoDB
val result = myShopifyProducts.syncToLocal(marketplace)
```

#### Push sync (ERP -> Shopify)

```kotlin
// 1. Implement IErpProductReader with your ERP data
class MyErpReader : IErpProductReader {
    override suspend fun readProducts(): List<Product> = /* query your ERP */
    override suspend fun readProductById(erpId: String): Product? = /* ... */
    override suspend fun readInventoryLevels(): Map<String, Int> = /* sku -> qty */
    override suspend fun readPriceLists(): Map<String, List<PriceListEntry>> = /* ... */
}

// 2. Create the sync engine
val engine = ShopifyBidirectionalSyncEngine(
    erpReader = MyErpReader(),
    pushService = ShopifyPushEngine(),
    listingsProvider = { marketplace -> listingsColl.findByMarketplace(marketplace) },
    listingSaver = { listing -> listingsColl.save(listing) },
)

// 3. Reconcile (dry run — see what changed)
val report = engine.reconcile(marketplace)
report.toCreate   // List<Product> — new products to push
report.toUpdate   // List<SyncDiff> — changed products with FieldChange details
report.orphaned   // List<ShopifyProduct> — exist in Shopify but not in ERP
report.inSync     // Int — count of unchanged products

// 4. Push changes
val result = engine.syncToRemote(marketplace)
```

#### Field-level diff

The diff engine compares 10+ fields and returns structured `FieldChange` objects:

```kotlin
val report = engine.reconcile(marketplace)
for (diff in report.toUpdate) {
    println("Product ${diff.shopifyId.id}:")
    for (change in diff.changedFields) {
        println("  ${change.fieldName}: ${change.oldValue} -> ${change.newValue}")
    }
}
// Output:
//   title: Old Name -> New Name
//   price: 199.99 -> 249.99
//   tags: [sale] -> [sale, featured]
```

#### Collection CRUD

```kotlin
val pushEngine = ShopifyPushEngine()

// Create a smart collection with rules
val collection = ShopifyCollection(
    title = "Summer Sale",
    handle = "summer-sale",
    rules = listOf(
        CollectionRule(column = "TAG", relation = "EQUALS", condition = "summer"),
    ),
    isSmartCollection = true,
)
pushEngine.createCollection(marketplace, collection)

// Update / delete
pushEngine.updateCollection(marketplace, updatedCollection)
pushEngine.deleteCollection(marketplace, "gid://shopify/Collection/123")
```

#### Image management

```kotlin
val pushEngine = ShopifyPushEngine()

// Upload from URL
pushEngine.uploadImage(marketplace, shopifyId, "https://example.com/photo.jpg", altText = "Product photo")

// Upload from bytes (3-step: staged upload -> HTTP PUT -> attach to product)
val processed = ImageProcessor.pipeline().resize(ImageSize.BASE).toJpeg().process(rawBytes)
pushEngine.uploadImageFromBytes(marketplace, shopifyId, processed, "photo.jpg", altText = "Processed photo")

// Delete and reorder
pushEngine.deleteImage(marketplace, shopifyId, mediaId)
pushEngine.reorderImages(marketplace, shopifyId, listOf(mediaId1, mediaId2, mediaId3))
```

#### Other operations

```kotlin
pushEngine.updateInventory(marketplace, shopifyId, locationId, quantity = 50)
pushEngine.setPublishState(marketplace, shopifyId, published = true)
pushEngine.addToCollection(marketplace, collectionId, shopifyProductId)
pushEngine.removeFromCollection(marketplace, collectionId, shopifyProductId)
pushEngine.getProduct(marketplace, shopifyId)  // -> ItemState<ShopifyProduct>
```

### MercadoLibre

Abstract fetch pattern — consumers provide the actual MercadoLibre API client.

#### Products

```kotlin
class MyMlProducts(db: MongoDatabase) : IMercadoLibreProductColl<ObjectId>(db) {
    override suspend fun fetchProducts(marketplace: Marketplace): List<MercadoLibreProduct> {
        val token = marketplace.connectionConfig["accessToken"]!!
        return mlApiClient.getProducts(token)  // your API client
    }
}

myMlProducts.syncToLocal(marketplace)  // calls fetchProducts, bulk upserts to MongoDB
```

#### Orders

```kotlin
class MyMlOrders(db: MongoDatabase) : IMercadoLibreOrderColl<ObjectId>(db) {
    override suspend fun fetchOrders(marketplace: Marketplace): List<MercadoLibreOrder> {
        val token = marketplace.connectionConfig["accessToken"]!!
        return mlApiClient.getOrders(token)
    }
}

myMlOrders.syncToLocal(marketplace)
```

`MercadoLibreOrder` includes: `buyer` (name, email, phone), `orderItems` (title, quantity, unitPrice, sku), `shipping` (status, tracking, address), `payments` (status, amount, method).

### Amazon

ASIN-based products. Abstract fetch pattern — consumers provide the SP-API client.

```kotlin
class MyAmazonProducts(db: MongoDatabase) : IAmazonProductColl<ObjectId>(db) {
    override suspend fun fetchProducts(marketplace: Marketplace): List<AmazonProduct> {
        val config = marketplace.connectionConfig
        return spApiClient.getCatalogItems(config)  // your SP-API client
    }
}

myAmazonProducts.syncToLocal(marketplace)
```

---

## Sync Engine

### Pull Sync (Marketplace -> Local)

All marketplace collections implement `IMarketplaceSync<T>`:

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState
    suspend fun syncToRemote(marketplace: Marketplace): SimpleState
}
```

Pull sync fetches data from the marketplace API and bulk-upserts into MongoDB.

### Push Sync (ERP -> Shopify)

The `ShopifyBidirectionalSyncEngine` handles the full push cycle: read ERP products, reconcile against existing Shopify listings, create/update/skip as needed, and persist listing state for future diffs.

### Bulk Operations

```kotlin
// Batch create/update with rate limiting
val result = engine.bulkSyncToRemote(
    marketplace = marketplace,
    batchSize = 50,       // products per batch
    delayMs = 500,        // ms between API calls
)
result.created  // Int
result.updated  // Int
result.failed   // Int
result.errors   // List<SyncError>
```

### Scheduler

`SyncScheduler` is a coroutine-based JVM scheduler for periodic sync tasks:

```kotlin
val scheduler = SyncScheduler()

val taskId = scheduler.schedule(SyncTask(
    marketplace = marketplace,
    syncEngine = shopifyProducts,
    interval = 30.minutes,
    onResult = { state -> logger.info("Sync: ${state.msgOk ?: state.msgError}") },
))

scheduler.isRunning(taskId)  // Boolean
scheduler.cancel(taskId)
scheduler.cancelAll()
scheduler.shutdown()
```

### Smoke Testing

Self-reverting round-trip test for verifying Shopify API connectivity:

```kotlin
val smokeTest = ShopifySmokeTest(ShopifyPushEngine())
val result = smokeTest.run(marketplace, shopifyProductId = "gid://shopify/Product/123")

result.updateVerified  // true if title change was confirmed
result.revertVerified  // true if original title was restored
result.error           // null on success
```

The test modifies a product title (appends `[SMOKE TEST]`), verifies the change via `getProduct`, then reverts. Uses `try/finally` to guarantee cleanup.

---

## Collections & Factory Pattern

Each entity has a model + container + collection triple. Extend the abstract factory to wire everything:

```kotlin
class MyCollections(db: MongoDatabase) : IShopifyCollections(db) {
    override val marketplaces       = MyMarketplaceColl(db)
    override val products           = MyProductColl(db)
    override val listings           = MyListingColl(db)
    override val costStructures     = MyCostStructureColl(db)
    override val costBreakdowns     = MyCostBreakdownColl(db)
    override val competitorPrices   = MyCompetitorPriceColl(db)
    override val ShopifyProductColl = MyShopifyProductColl(db)
    override val ShopifyCollectionColl = MyShopifyCollectionColl(db)
    override val ShopifyBusinessEntityColl = MyShopifyBusinessEntityColl(db)
}
```

For MercadoLibre or Amazon, extend `IMercadoLibreCollections` or `IAmazonCollections` instead.

---

## KVision Building Blocks (JS)

The `commonLib` JS source set provides reusable KVision components for building admin UIs:

| Type | Description | Example |
|------|-------------|---------|
| **Column definitions** (`colDef`) | Typed Tabulator columns with headerFilter/headerSort/formatters | `colDefMarketplaceName`, `colDefProductSku`, `colDefHealthLevel` |
| **Filter views** (`filter`) | Off-canvas filter panels bound to typed filter classes | `filterMarketplace`, `filterMarketplaceListing` |
| **Form builders** (`form`) | Form inputs bound to entity fields | `formMarketplace`, `formProduct`, `formCompetitorPrice` |

Use them with fsLib's `ViewList`/`ViewItem` pattern:

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
val filter = ProductFilter(sku = "ABC-123", publishEnabled = true)
val updated = filter.copy(status = "active")
```

Available: `MarketplaceFilter`, `ProductFilter`, `MarketplaceListingFilter`, `CostStructureFilter`, `CostBreakdownFilter`, `CompetitorPriceFilter`, `ShopifyProductFilter`, `ShopifyCollectionFilter`, `MercadoLibreProductFilter`, `MercadoLibreOrderFilter`, `AmazonProductFilter`.

---

## Sample Application

A runnable full-stack demo using `InMemoryRepository` (no MongoDB required):

```bash
./gradlew :samples:run
# Ktor server at http://localhost:8080
```

Seed data includes:
- 3 Marketplaces (Shopify, MercadoLibre, Amazon) with Mexican pricing config
- 3 Products with variants, presentations, and package dimensions
- 6 Marketplace listings with publicationType and presentationIndex
- Cost structures, breakdowns, and competitor prices

Frontend: KVision app with list and detail views for all entities.

---

## Build & Test

```bash
./gradlew build                   # Build all modules
./gradlew test                    # Run all tests
./gradlew jvmTest                 # JVM tests only
./gradlew :commonLib:jvmTest      # Money + pricing service tests
./gradlew :shopifyLib:jvmTest     # Shopify annotation + sync engine tests
./gradlew publishToMavenLocal     # Publish to local Maven repo

# Run a single test
./gradlew :commonLib:jvmTest --tests "PricingServiceTest.profit calculator with dual commissions"
```

Test coverage includes: `Money` arithmetic, `ProfitCalculator` (IVA/ISR withholding, dual commissions, cargo fijo, envio logistico, cost x pieces scaling, referralKitCost, NO_DATA health), `ShippingCalculator` (volumetric weight, tier resolution, overrides), `PresentationComparator`, `MarketplaceDefaults`, `MarketplaceComparator`, `PriceSuggester`, `SyncScheduler`, `@ShopifyField` annotation mappings, `ShopifyBidirectionalSyncEngine` reconcile/push logic.

---

## Dependencies

| Library | Version | Role |
|---------|---------|------|
| Kotlin Multiplatform | 2.3.20 | Language & targets |
| fsLib | 3.2.0 | BaseDoc, Coll, InMemoryRepository, ViewList/ViewItem |
| KVision | 9.5.0 | UI framework (JS target) |
| Kilua RPC | 0.0.43 | Type-safe RPC for KMP |
| Apollo GraphQL | 4.4.2 | Shopify Admin API client |
| Dokka | 2.2.0 | API documentation (V2 plugin) |
| kotlinx-serialization | 1.10.0 | Serialization |
| kotlinx-datetime | 0.6.1 | Date/time |

---

## Architecture Notes

- **Money as cents** — All monetary values use `Money(Long)`. Never `Double` or `BigDecimal`.
- **Typed IDs** — `OId<T>` for MongoDB entities, `StringId<T>` for marketplace entities. Compile-time type safety prevents mixing IDs.
- **Marketplace credentials** — Stored as `Map<String, String>` in `Marketplace.connectionConfig`. Keys vary by marketplace type (e.g., `url`, `apiVersion`, `accessToken` for Shopify).
- **Mexican marketplace pricing** — Full cost modeling for MercadoLibre and Amazon Mexico: dual commission rates (clasica/premium), cargo fijo tiers, envio logistico with volumetric weight, price-based shipping discounts, IVA withholding (8%), ISR withholding (2.5%), per-piece cost scaling.
- **Abstract fetch pattern** — mercadoLibreLib and amazonLib define `abstract suspend fun fetchProducts()`. Consumers implement with their own API client. The library handles bulk upsert, sync state, and error reporting.
- **Bidirectional sync** — `ShopifyBidirectionalSyncEngine` reconciles ERP products against Shopify listings, detects 10+ field changes, and pushes creates/updates with configurable batching and rate limiting.
- **Multiplatform safety** — All domain logic lives in `commonMain`. MongoDB collections, Apollo clients, image processing, and coroutine-heavy sync code live in `jvmMain`. JS source sets contain only KVision UI building blocks.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## License

Copyright Fonrouge. All rights reserved.
