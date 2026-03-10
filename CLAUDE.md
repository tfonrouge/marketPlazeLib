# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build all modules
./gradlew test               # Run all tests
./gradlew jvmTest            # Run JVM tests only
./gradlew :shopifyLib:jvmTest  # Run shopifyLib tests specifically
./gradlew publishToMavenLocal  # Publish to local Maven repo
```

## Architecture

**marketPlazeLib** is a Kotlin Multiplatform library providing marketplace integration abstractions. It targets JVM and JavaScript (ES2015, IR compiler).

### Modules

- **commonLib** — Platform-agnostic interfaces for marketplace concepts (`IMarketProduct`, `IMarketProductId`, `MarketPlaceId` enum). Defines the abstraction layer that marketplace-specific modules implement.
- **shopifyLib** — Shopify-specific implementation using Apollo GraphQL against the Shopify Admin API. Contains GraphQL schema/queries in `src/commonMain/graphql/`, KVision UI components in `src/jsMain/`, and database collection interfaces in `src/jvmMain/`.

### Key Patterns

- Generic product ID pattern: `IMarketProductId<T>` allows each marketplace to define its own ID type
- `@ShopifyField` annotation maps Kotlin properties to GraphQL node fields; tests validate these mappings via reflection
- JVM source sets contain database collection interfaces (prefixed with `I*Coll`)
- JS source sets contain KVision view components
- GraphQL schema file (`shopify-admin-2025-10.graphqls`) is large (~3.2MB); Apollo generates Kotlin models from it

### Dependencies

Core dependencies are managed via `gradle/libs.versions.toml`. Key ones:
- **fsLib** (Fonrouge full-stack library) — internal dependency providing base infrastructure
- **Kilua RPC** — RPC framework
- **KVision** — UI framework for JS target
- **Apollo GraphQL** — GraphQL client for Shopify API
- **kotlinx-serialization** and **kotlinx-datetime** — serialization and date/time

Published as Maven artifacts under group `com.fonrouge.marketPlazeLib` (version 1.0.1).