# BRIEF — FullShopifySync (FEATURE)

## Kickoff

| Campo | Detalle |
|-------|---------|
| **Feature Name** | `FullShopifySync` |
| **Target Library** | `marketPlazeLib` v2.0.0 |
| **Affected Modules** | shopifyLib (collection CRUD, diff engine, bulk ops, binary image upload), commonLib (image processing pipeline) |
| **Business Owner** | Library maintainer |
| **Business Justification** | shopifyLib currently supports product CRUD, inventory, and media management via GraphQL, but lacks smart collection lifecycle management, server-side image processing before upload, comprehensive field-level diff detection, and bulk sync operations. These are essential for any production ERP→Shopify integration. |
| **Key Stakeholders** | Any consumer application implementing `IErpProductReader` for ERP→Shopify sync |
| **Target Version** | 2.0.0 (major bump due to breaking `SyncDiff.changedFields` type change) |
| **Current Version** | 1.3.0 |

## What Already Exists (v1.3.0)

- **Shopify**: Apollo GraphQL client (2025-10), ShopifyPushEngine (10 mutations: product CRUD, inventory, add/remove from collections, images, publish), ShopifyBidirectionalSyncEngine (reconcile + push), paginated sync
- **Pricing**: ProfitCalculator (dual commissions, IVA/ISR withholding, cargo fijo, envio tiers), ShippingCalculator, PriceSuggester, MarketplaceComparator
- **Models**: Money (value class), Marketplace, Product (variants, presentations, images), MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice
- **Sync**: IMarketplaceSync<T>, IErpProductReader, SyncScheduler
- **KVision**: 21 column definitions, 3 filter views, 3 form builders
- **Tests**: 43 tests across all modules

## What Needs to Be Added for v2.0.0

| Feature | Gap | Module |
|---------|-----|--------|
| Smart collection CRUD | Only add/remove products; no create/update/delete collections themselves | shopifyLib |
| Collection rules model | `ShopifyCollection` lacks `rules`, `isSmartCollection`, `image` | shopifyLib |
| Image processing pipeline | Only uploads by URL; no resize/watermark/format conversion | commonLib (JVM) |
| Binary image upload | No `stagedUploadsCreate` mutation for byte array upload | shopifyLib |
| Field-level diff engine | `detectChanges()` only compares title + price; production use requires 10+ field comparison | shopifyLib |
| Bulk sync operations | One-at-a-time create/update; no batch support | shopifyLib |
| Dependency update | Kotlin 2.3.10→2.3.20, fsLib 3.2.0-SNAPSHOT→3.2.0, KVision 9.4.0-SNAPSHOT→9.5.0, kilua-rpc 0.0.42→0.0.43, KSP 2.3.5→2.3.6 | all |

## Out of Scope

- MercadoLibre API client implementation (stays abstract)
- Amazon SP-API client implementation (stays abstract)
- Webhook integration
- Multi-tenant isolation
- Revenue/profit reporting dashboards
- Consumer-specific ERP adapters (consumers implement `IErpProductReader` themselves)

---

[← Index](../INDEX.md) · **BRIEF** · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · PLAN · TESTS · AUDIT
