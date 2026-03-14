# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build all modules
./gradlew test               # Run all tests
./gradlew jvmTest            # Run JVM tests only
./gradlew :commonLib:jvmTest # Run commonLib tests (Money, pricing services)
./gradlew :shopifyLib:jvmTest  # Run shopifyLib tests specifically
./gradlew :shopifyLib:jvmTest --tests "ShopifyTest.shopify product properties test"  # Run a single test
./gradlew publishToMavenLocal  # Publish to local Maven repo
```

## Architecture

**marketPlazeLib** is a Kotlin Multiplatform library providing marketplace intelligence abstractions: multi-marketplace product management, cost structure modeling, profit calculation, competitive pricing, and sync interfaces. Targets JVM and JavaScript (ES2015, IR compiler).

### Modules

- **commonLib** — Core domain: entity models (`Marketplace`, `Product`, `MarketplaceListing`, `CostStructure`, `CostBreakdown`, `CompetitorPrice`), `Money` value class, pricing services (`ProfitCalculator`, `MarketplaceComparator`, `PriceSuggester`), `IMarketplaceSync` interface, `SyncScheduler`, abstract `ICommonContainer` and `Coll` classes, `MarketPlazeCollections` factory, KVision view components (marketplace, product, cost structure, profit dashboard, competitor prices).
- **shopifyLib** — Shopify-specific implementation using Apollo GraphQL against the Shopify Admin API. Implements `IMarketplaceSync`. Contains GraphQL schema/queries in `src/commonMain/graphql/`, KVision UI components in `src/jsMain/`, and database collection classes in `src/jvmMain/`.
- **mercadoLibreLib** — MercadoLibre marketplace integration. `MercadoLibreProduct` model, abstract `IMercadoLibreProductColl` with `fetchProducts()` hook implementing `IMarketplaceSync`. Consumers provide the actual MercadoLibre API client.
- **amazonLib** — Amazon marketplace integration. `AmazonProduct` model with ASIN-based identification, abstract `IAmazonProductColl` with `fetchProducts()` hook implementing `IMarketplaceSync`. Consumers provide the actual Amazon SP-API client.

### Key Patterns

- **Entity triple**: Each entity requires 3 files: model (`BaseDoc`), container (`ICommonContainer` subclass), and collection (`Coll` subclass in jvmMain)
- **Money as cents**: `Money` is a `@JvmInline value class` wrapping `Long` — exact arithmetic, KMP-safe
- **Marketplace connection**: `Marketplace.connectionConfig: Map<String, String>` stores credentials generically per marketplace type
- **IMarketplaceSync interface**: Common sync contract in commonLib; implemented by `IShopifyColl`, `IMercadoLibreProductColl`, and `IAmazonProductColl`
- **SyncScheduler**: Coroutine-based scheduler (JVM) that runs `IMarketplaceSync` tasks at configurable intervals
- **Abstract fetch pattern**: mercadoLibreLib and amazonLib define abstract `fetchProducts(marketplace)` — consumers implement with their API client
- `@ShopifyField` annotation maps Kotlin properties to GraphQL node fields; tests validate these mappings via reflection
- JVM source sets contain abstract `Coll` classes for MongoDB with bulk upsert sync patterns
- JS source sets contain KVision view components
- GraphQL schema file (`shopify-admin-2025-10.graphqls`) is large (~3.2MB); Apollo generates Kotlin models from it
- `MarketPlazeCollections` is the base abstract factory; `IShopifyCollections` extends it adding Shopify-specific collections

### Dependencies

Core dependencies are managed via `gradle/libs.versions.toml`. Key ones:
- **fsLib** (Fonrouge full-stack library) — internal dependency providing BaseDoc, ICommonContainer, Coll, OId, StringId
- **Kilua RPC** — RPC framework
- **KVision** — UI framework for JS target
- **Apollo GraphQL** (4.3.3) — GraphQL client for Shopify API; configured as Apollo service "shopify" in package `com.fonrouge`
- **kotlinx-serialization** and **kotlinx-datetime** — serialization and date/time

Published as Maven artifacts under group `com.fonrouge.marketPlazeLib` (version managed in `gradle/libs.versions.toml`).
