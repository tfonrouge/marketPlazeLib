# Decision Ledger — ShopifyPushSync

| Date | Decision | Type | Rationale | Evidence required to revisit |
|------|----------|------|-----------|------------------------------|
| 2026-03-16 | Bridge lifecycle = **Permanent** | APPROVED | Shopify is a first-class supported marketplace; not a temporary integration | Shopify deprecates Admin GraphQL; OR marketPlazeLib gains a non-Shopify push target requiring the engine to be generalized (then re-evaluate as MODULE/LIBRARY rather than BRIDGE) |
| 2026-03-16 | `IErpProductReader` is consumer-implemented (commonLib defines, app provides body) | APPROVED | Consumer owns ERP shape; library cannot anticipate every ERP | More than two consumers ship; common ERP adapters justify a `commonErpAdapters` module |
| 2026-03-16 | All operations are synchronous request/response (no domain events emitted) | APPROVED | Single-tenant per consumer; no event bus exists; simplicity > eventual consistency | Multi-consumer/multi-tenant pushes start contending; OR a consumer needs sync-to-multiple-marketplaces choreography |
| 2026-03-16 | `ShopifyPushEngine` carries its own `buildApolloClient` (not coupled to `IShopifyColl`) | APPROVED | Push engine should be invocable without a Coll bound (e.g., one-shot CLI publish) | Apollo client config diverges between pull and push paths and the duplication causes config drift |
| 2026-05-01 | Image upload via `productUpdate` with `media` param (replacing deprecated `productCreateMedia`); image delete via `fileDelete` (replacing `productDeleteMedia`) | APPROVED | Shopify deprecated the original mutations on Admin API 2025-10 | Shopify deprecates `productUpdate.media` or `fileDelete`; OR the new path lacks a feature the old one had (e.g., position control) |
| 2026-05-01 | `IErpProductReader.readInventoryByLocation()` defaults to empty map for backward compatibility | APPROVED | Add capability without breaking pre-2.3 consumers | Default empty map hides bugs (consumers fail silently when they should set up multi-location inventory); OR the default-empty path becomes the documented "single-location" mode and the empty-as-sentinel becomes ambiguous |

## Standing Rejections
_None recorded._

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [AUDIT](AUDIT.md)
