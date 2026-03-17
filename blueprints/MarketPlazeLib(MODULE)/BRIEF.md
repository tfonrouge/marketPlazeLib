# MarketPlazeLib — Project Brief

| Field | Value |
|-------|-------|
| Library Name | `MarketPlazeLib` |
| Business Owner | Teo Fonrouge |
| Business Justification | Replace the single-marketplace Shopify-only sync library with a multi-marketplace intelligence platform: unified product management, cost structure modeling, profit calculation, competitive pricing, and pluggable sync for Shopify/MercadoLibre/Amazon |
| Platform/Runtime Targets | KMP — JVM + JS (ES2015, IR compiler) |
| Published Artifacts | Maven `com.fonrouge.marketPlazeLib:commonLib`, `:shopifyLib`, `:mercadoLibreLib`, `:amazonLib`; `samples` (demo only — InMemoryRepository, Ktor + KVision, 14 integration tests) |
| Consumer Modules | **mppErsaPack** (primary consumer) |
| Integration Surface | **fsLib** 3.1.1 (infrastructure dependency) |
| Target Go-Live | v1.0.0 — shipped 2026-03-15 |

## Status

**COMPLETED.** All phases shipped. Published as Maven artifacts v1.0.0 under `com.fonrouge.marketPlazeLib`.

## Scope

- **commonLib** — Core domain: 6 entities (Marketplace, Product, MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice), Money value class, typed filters, `data object` CommonContainers, abstract Coll classes, MarketPlazeCollections factory, pricing services (ProfitCalculator, MarketplaceComparator, PriceSuggester), IMarketplaceSync interface, SyncScheduler, KVision view building blocks (column definitions, filter views, form builders)
- **shopifyLib** — Shopify pull sync via Apollo GraphQL, ShopifyProduct, ShopifyBusinessEntity, @ShopifyField, ShopifyProductFilter, IShopifyCollections
- **mercadoLibreLib** — MercadoLibreProduct model, abstract IMercadoLibreProductColl with fetchProducts() hook
- **amazonLib** — AmazonProduct model, abstract IAmazonProductColl with fetchProducts() hook
- **samples** — Runnable sample app with InMemoryRepository, Ktor server, KVision frontend, 14 integration tests

## Out of Scope (handled by separate blueprints)

- ERP→Shopify push sync — see `ShopifyPushSync(BRIDGE)`
- MercadoLibre real API integration — future BRIDGE
- Amazon SP-API integration — future BRIDGE
- Production deployment configuration

## Key Decisions

- **Money**: `Long` (cents) `@JvmInline value class` — exact arithmetic, KMP-safe
- **Modules**: 5 Gradle modules (commonLib, shopifyLib, mercadoLibreLib, amazonLib, samples)
- **Connection config**: Generic `Map<String, String>` per marketplace
- **CommonContainers**: Concrete `data object` singletons with `filterKClass` (not abstract classes)
- **Coll signature**: 4 type params `<T, ID, FILT, UID>` (fsLib 3.1.1)
- **No backward compatibility**: Breaking change from pre-release versions
- **fsLib**: 3.1.1 (stable release)

## Architecture

```
commonLib (models, pricing services, sync abstractions, KVision building blocks)
    ↑
shopifyLib (Shopify GraphQL pull sync, @ShopifyField)
    ↑
mercadoLibreLib (MercadoLibre model + abstract fetch)
amazonLib (Amazon model + abstract fetch)
samples (runnable sample app with InMemoryRepository)
```

---
[← Index](../INDEX.md) · **BRIEF** · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · [PLAN](IMPLEMENTATION_PLAN.md) · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [AUDIT](AUDIT.md)
