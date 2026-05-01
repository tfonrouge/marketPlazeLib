# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Premise: cathedral
Follow the cathedral premise.

### Project config
- Blueprint skill: business-blueprint-workflow
- Blueprint root: blueprints/
- Build verification: `./gradlew build` (all modules); `./gradlew test` for the full test suite
- Project language: Kotlin Multiplatform (JVM + JS / ES2015, IR)
- Test formats: unit tests (`*Test.kt`), integration tests (`ShopifySmokeTest`, `SampleIntegrationTest`)

## Build Commands

```bash
./gradlew build              # Build all modules
./gradlew test               # Run all tests
./gradlew jvmTest            # Run JVM tests only
./gradlew :commonLib:jvmTest # Run commonLib tests (Money, pricing services)
./gradlew :shopifyLib:jvmTest  # Run shopifyLib tests specifically
./gradlew :shopifyLib:jvmTest --tests "ShopifyTest.shopify product properties test"  # Run a single test
./gradlew :samples:run       # Run sample app (InMemoryRepository, no MongoDB needed)
./gradlew publishToMavenLocal  # Publish to local Maven repo
```

## Architecture

**marketPlazeLib** is a Kotlin Multiplatform library providing marketplace intelligence abstractions: multi-marketplace product management, cost structure modeling, profit calculation, competitive pricing, and sync interfaces. Targets JVM and JavaScript (ES2015, IR compiler).

### Modules

- **commonLib** — Core domain: entity models (`Marketplace`, `Product`, `MarketplaceListing`, `CostStructure`, `CostBreakdown`, `CompetitorPrice`), `Money` value class, pricing services (`ProfitCalculator`, `MarketplaceComparator`, `PriceSuggester`, `ShippingCalculator`, `PresentationComparator`, `MarketplaceDefaults`), `IMarketplaceSync` interface, `SyncScheduler`, `data object` CommonContainers, abstract `Coll` classes, `MarketPlazeCollections` factory, KVision view building blocks (column definitions, filter views, form builders).
- **shopifyLib** — Shopify-specific implementation using Apollo GraphQL against the Shopify Admin API. Implements `IMarketplaceSync`. Contains GraphQL schema/queries in `src/commonMain/graphql/` and database collection classes in `src/jvmMain/`.
- **mercadoLibreLib** — MercadoLibre marketplace integration. `MercadoLibreProduct` model, abstract `IMercadoLibreProductColl` with `fetchProducts()` hook implementing `IMarketplaceSync`. Consumers provide the actual MercadoLibre API client.
- **amazonLib** — Amazon marketplace integration. `AmazonProduct` model with ASIN-based identification, abstract `IAmazonProductColl` with `fetchProducts()` hook implementing `IMarketplaceSync`. Consumers provide the actual Amazon SP-API client.
- **samples** — Runnable sample app consuming the library. Uses fsLib's `InMemoryRepository` (no MongoDB). Ktor server on port 8080, KVision frontend with ViewList/ViewItem classes, seed data. Run with `./gradlew :samples:run`.

### Key Patterns

- **Entity triple**: Each entity has 3 parts: model (`BaseDoc`), container (`data object` extending `ICommonContainer`), and collection (`Coll` subclass in jvmMain)
- **Money as cents**: `Money` is a `@JvmInline value class` wrapping `Long` — exact arithmetic, KMP-safe
- **Marketplace connection**: `Marketplace.connectionConfig: Map<String, String>` stores credentials generically per marketplace type
- **IMarketplaceSync interface**: Common sync contract in commonLib; implemented by `IShopifyColl`, `IMercadoLibreProductColl`, and `IAmazonProductColl`
- **SyncScheduler**: Coroutine-based scheduler (JVM) that runs `IMarketplaceSync` tasks at configurable intervals
- **Abstract fetch pattern**: mercadoLibreLib and amazonLib define abstract `fetchProducts(marketplace)` — consumers implement with their API client
- **Typed filters**: Each entity has a concrete `IApiFilter` subclass (e.g., `MarketplaceFilter`, `ProductFilter`) with immutable `copy()` pattern for view updates
- `@ShopifyField` annotation maps Kotlin properties to GraphQL node fields; tests validate these mappings via reflection
- JVM source sets contain abstract `Coll` classes for MongoDB with bulk upsert sync patterns
- JS source sets contain reusable KVision building blocks: column definitions (`colDef`), off-canvas filter views (`filter`), form field builders (`form`)
- GraphQL schema file (`shopify-admin-2025-10.graphqls`) is large (~3.2MB); Apollo generates Kotlin models from it
- `MarketPlazeCollections` is the base abstract factory; `IShopifyCollections` extends it adding Shopify-specific collections
- **View pattern**: ViewList/ViewItem classes use companion object `configViewList`/`configViewItem` with `ViewRegistry` for service manager binding (see fsLib showcase and samples module)
- **Mexican marketplace pricing**: `Marketplace` supports dual commission rates (classic/premium), `CargoFijoTier`, `EnvioTier`, `EnvioDiscount` for Mexican marketplace cost modeling. `ShippingCalculator` handles volumetric weight (`L×W×H/5000`), tier-based shipping costs, and price-based discounts. `MarketplaceDefaults` provides pre-configured ML Mexico (21 envío tiers, 3 discounts, 4 cargo fijo tiers) and Amazon Mexico defaults.
- **Publication types**: `MarketplaceListing.publicationType` (CLASICA/PREMIUM) determines which commission rate applies. `ProfitCalculator` resolves commission from marketplace dual rates or falls back to `CostStructure.commissions[]`.
- **PresentationComparator**: Compares multiple `ProductPresentation` entries for a product, ranking by `profitPerPiece`
- **HealthLevel.NO_DATA**: Returned when `baseCost <= 0` — distinguishes missing cost data from genuinely bad margins

### Dependencies

All dependencies and versions are managed in `gradle/libs.versions.toml` — that is the source of truth. Key roles:
- **fsLib** (Fonrouge full-stack library) — BaseDoc, ICommonContainer, Coll, InMemoryRepository, ViewList/ViewItem, OId, StringId
- **Kilua RPC** — RPC framework for service interface generation
- **KVision** — UI framework for JS target
- **Apollo GraphQL** — GraphQL client for Shopify API; configured as Apollo service "shopify" in package `com.fonrouge`
- **kotlinx-serialization** and **kotlinx-datetime** — serialization and date/time

Published as Maven artifacts under group `com.fonrouge.marketPlazeLib` (version managed in `gradle/libs.versions.toml`).
