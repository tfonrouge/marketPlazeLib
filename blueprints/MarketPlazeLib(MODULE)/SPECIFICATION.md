# MarketPlazeLib — Technical Specification

## Module Objective & Scope

MarketPlazeLib is a Kotlin Multiplatform library (JVM + JS) providing domain models, pricing intelligence, and sync abstractions for multi-marketplace product management. It replaces the v1.x Shopify-only sync library with a marketplace-agnostic architecture where each marketplace (Shopify, MercadoLibre, Amazon) implements a common sync interface. The pricing engine accounts for Mexican marketplace tax withholdings (IVA, ISR), discount handling, shipping tier resolution, and per-piece analysis. Consuming apps (e.g., mppErsaPack) depend on this library for all marketplace-related data modeling, cost analysis, and UI building blocks.

## Functional Requirements

- FR-01: The system SHALL represent monetary values as `Long` (cents) via a `Money` value class with exact arithmetic operators (+, -, ×, ÷, unaryMinus, compareTo)
- FR-02: The system SHALL model marketplaces with generic `connectionConfig: Map<String, String>` and tax withholding rates (`ivaRate`, `isrRate`, `ivaIncluded`)
- FR-03: The system SHALL model products with SKU, name, base cost, variants, images, and presentations
- FR-04: The system SHALL model marketplace listings with external ID, price, publish state, discount, and listing URL
- FR-05: The system SHALL model cost structures per marketplace with commissions (% + fixed), fees, shipping tiers (price-based or weight-based), advertising rates, and publication modalities
- FR-06: The system SHALL calculate cost breakdowns including: discount → final price, taxable base, IVA/ISR withholdings, shipping tier resolution, commissions, fees, modalities, health classification, and optional per-piece metrics
- FR-07: The system SHALL compare the same product across N marketplaces (each with its own marketplace + cost structure pair) and rank by net profit and profit margin
- FR-08: The system SHALL suggest minimum price for a target margin, accounting for commissions and tax withholdings
- FR-09: The system SHALL suggest competitive prices from competitor observations using strategies (match lowest, match average, below average)
- FR-10: The system SHALL define a common sync interface `IMarketplaceSync<T>` with `syncToLocal(marketplace): SimpleState`
- FR-11: The system SHALL provide a coroutine-based `SyncScheduler` for periodic sync tasks with schedule/cancel/shutdown lifecycle
- FR-12: The system SHALL provide abstract collection classes (Coll subclasses) for MongoDB persistence of all entities
- FR-13: The system SHALL provide concrete `data object` CommonContainers with `filterKClass` for each entity
- FR-14: The system SHALL provide typed filter classes with immutable `copy()` update pattern
- FR-15: The system SHALL provide reusable KVision column definitions with Tabulator headerFilter/headerSort/formatters
- FR-16: The system SHALL provide off-canvas filter view builders as extension functions on `ViewList`
- FR-17: The system SHALL provide form field builders using `formRow`/`formColumn` layout from fsLib
- FR-18: The system SHALL implement Shopify pull sync via Apollo GraphQL with cursor-based pagination and bulk MongoDB upsert
- FR-19: The system SHALL validate `@ShopifyField` annotation mappings against Apollo-generated GraphQL node properties via reflection tests
- FR-20: The system SHALL provide abstract `fetchProducts(marketplace)` hooks for MercadoLibre and Amazon integrations
- FR-21: The system SHALL resolve shipping costs from price-based tier lists, returning Money.ZERO when price exceeds all tiers (free shipping)
- FR-22: The system SHALL classify profit margins into health levels: Critical (<10%), Acceptable (10-15%), Healthy (15-30%), Excellent (≥30%)
- FR-23: The system SHALL model product presentations (piece → package → display) with packaging cost and per-piece metric calculation

## Business Rules & Constraints

- BR-01: Money arithmetic MUST NOT use floating point — all calculations use `Long` (cents)
- BR-02: Commission calculation: `amount = finalPrice.cents × percentage / 100 + fixedAmount.cents`
- BR-03: Profit margin: `profitMarginPercent = netProfit.cents / finalPrice.cents × 100`
- BR-04: PriceSuggester returns `Long.MAX_VALUE` when target margin is impossible (denominator ≤ 0)
- BR-05: SyncScheduler survives individual sync failures — errors are captured in `onResult` callback, the scheduler continues running
- BR-06: SyncScheduler rescheduling the same marketplace+engine replaces the previous task
- BR-07: Shopify sync uses Shopify Admin GraphQL API with `X-Shopify-Access-Token` header
- BR-08: `@ShopifyField(ignore = true)` excludes fields from GraphQL mapping
- BR-09: `finalPrice = salePrice × (1 - discount)` — discount applied before any fee calculation
- BR-10: `taxableBase = finalPrice / 1.16` when `ivaIncluded = true`
- BR-11: `ivaWithheld = taxableBase × ivaRate` — IVA withholding (8% for MX marketplaces)
- BR-12: `isrWithheld = taxableBase × isrRate` — ISR withholding (2.5% for MX marketplaces)
- BR-13: Shipping tier resolution: match finalPrice against tiers where `tierType = "price"` and `minValue ≤ price < maxValue`; Money.ZERO if no match (free shipping)
- BR-14: Per-piece metrics calculated only when `piecesCount > 0`

## User Roles & Permissions

Library — role management delegated to consuming app. The library provides:
- Abstract `Coll` classes with `userCollFun` hook for RBAC
- Filter classes with `masterItemId` for hierarchical access control
- ViewList/ViewItem patterns integrating with fsLib's permission system

## Data Models & Relationships

### Entities

| Entity | ID Type | Module | Key Fields |
|--------|---------|--------|------------|
| `Marketplace` | `OId<Marketplace>` | commonLib | name, type, connectionConfig, enabled, ivaRate, isrRate, ivaIncluded |
| `Product` | `OId<Product>` | commonLib | sku, name, baseCost, variants, images, presentations |
| `MarketplaceListing` | `OId<MarketplaceListing>` | commonLib | productId↗, marketplaceId↗, externalId, title, price, publishEnabled, discount, listingUrl |
| `CostStructure` | `OId<CostStructure>` | commonLib | marketplaceId↗, name, commissions, fees, shippingTiers, advertisingRates, publicationModalities |
| `CostBreakdown` | `OId<CostBreakdown>` | commonLib | productId↗, marketplaceId↗, listingId↗?, salePrice, finalPrice, baseCost, taxableBase, ivaWithheld, isrWithheld, shippingCost, lineItems, totalCosts, netProfit, profitMarginPercent, healthLevel, piecesCount, pricePerPiece, costPerPiece, profitPerPiece |
| `CompetitorPrice` | `OId<CompetitorPrice>` | commonLib | marketplaceId↗, productId↗?, competitorName, price, observedAt |
| `ShopifyProduct` | `StringId` | shopifyLib | marketplaceId↗, handle, title, description, descriptionHtml, vendor, totalInventory, media |
| `ShopifyBusinessEntity` | `StringId` | shopifyLib | archived, companyName, displayName, primary |
| `MercadoLibreProduct` | `StringId` | mercadoLibreLib | marketplaceId↗, title, categoryId, price, currencyId, pictures, status |
| `AmazonProduct` | `StringId` | amazonLib | marketplaceId↗, asin, title, brand, category, price, images |

### Value Types

| Type | Key Fields |
|------|------------|
| `Money` | `cents: Long` |
| `Commission` | name, percentage, fixedAmount |
| `Fee` | name, amount, type |
| `ShippingTier` | name, minValue, maxValue, tierType ("price"/"weight"), cost |
| `AdvertisingRate` | name, costPerClick, costPerImpression |
| `PublicationModality` | name, fee, features |
| `FeeLineItem` | name, amount, type |
| `ProductVariant` | sku, name, price, inventory |
| `ProductImage` | url, altText, width, height |
| `ProductPresentation` | label, piecesPerPackage, packagesPerDisplay, totalPieces, packagingCost, isHypothetical |
| `PriceListEntry` | name, price, currencyCode, compareAtPrice |
| `HealthLevel` | CRITICAL/ACCEPTABLE/HEALTHY/EXCELLENT with classify() |

## Integration Points

| Consumer | Data Exchanged | Direction |
|----------|---------------|-----------|
| **mppErsaPack** | All entities, pricing services, Shopify sync, collections | consumes library |
| **fsLib 3.1.1** | BaseDoc, ICommonContainer, Coll, InMemoryRepository, ViewList/ViewItem, OId, StringId | infrastructure dependency |
| **Apollo GraphQL** | Shopify Admin API queries | shopifyLib → Shopify |

## Performance Requirements

- PR-01: Pricing calculations MUST execute in <1ms for single product (pure functions, no I/O)
- PR-02: Shopify sync MUST handle 250 products per GraphQL page
- PR-03: SyncScheduler MUST support concurrent sync tasks without blocking

## Security Considerations

- SC-01: Marketplace credentials stored in MongoDB — consuming app responsible for encryption at rest
- SC-02: Shopify access tokens via HTTPS only
- SC-03: No credentials logged or serialized in error messages
- SC-04: `userCollFun` hook enables RBAC

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · **SPEC** · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · [PLAN](IMPLEMENTATION_PLAN.md) · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [AUDIT](AUDIT.md)
