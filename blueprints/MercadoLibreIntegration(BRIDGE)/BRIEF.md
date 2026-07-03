# MercadoLibreIntegration — Bridge Brief

| Field | Value |
|-------|-------|
| Feature Name | `MercadoLibreIntegration` |
| Business Problem | mercadoLibreLib defines `IMercadoLibreProductColl` with an abstract `fetchProducts()` hook (pull direction) but has no push capability at all — there is no way for a consumer to update a MercadoLibre listing's stock from the library. Consumers (today: mppErsaPack) built their own read-side API client ad hoc rather than through the library, and had zero way to push corrected stock back to MercadoLibre when ERP inventory changes — an audit of mppErsaPack in this session (2026-07-03) confirmed a live consequence: a MercadoLibre listing showing stale `available_quantity` after real stock changed, with no mechanism to correct it (findings not yet recorded in mppErsaPack's own blueprint — that write-up is a separate, pending step, tracked outside this bridge). This bridge adds the missing push primitive; the pre-existing pull gap (concrete `fetchProducts` client) remains a separate, still-open item — see Out of Scope. |
| Affected Modules | **mercadoLibreLib** (new `IMercadoLibrePushService` + `MercadoLibrePushEngine`), **consuming apps** (orchestrate: read ERP stock, match to a MercadoLibre item id, call the push service) |
| New Entities | None — no new persisted entity. `IMercadoLibrePushService` is a service interface over the existing `Marketplace` entity; no change to `MercadoLibreProduct`. |
| Out of Scope | Moving the existing read-side `MercadoLibreApiClient` (fetchProducts/fetchOrders/OAuth PKCE/token refresh) from mppErsaPack into this library — that is a separate, higher-risk migration (touches live prod code) and is not required to ship the push primitive. MercadoLibre order management, shipping integration, advertising API, webhook listeners (unchanged from original scope). Automatic token refresh on 401 (the push engine surfaces the 401, it does not refresh — see SERVICE_CONTRACTS). Wiring a consumer in mppErsaPack (tracked in `mppErsaPack/blueprints/MarketPlace(MODULE)`, a different repo/blueprint tree — this bridge's Definition of Done is the library-side capability, not the app-side consumption). |
| Owner | Teo Fonrouge |
| Target | Library-side shipped 2026-07-03; consumer wiring in mppErsaPack pending (separate blueprint) |
| **Library Version Target** | **MarketPlazeLib v2.7.0** |
| **Lifecycle Type** | **Permanent** |
| **Status** | 🟡 ACTIVE — `IMercadoLibrePushService`/`MercadoLibrePushEngine` implemented and tested (6/6 tests passing, `./gradlew build` green); not STABLE until a real consumer wires it, per the orphan-code lesson recorded in LEDGER D5 (`ShopifyPushEngine`/`IShopifyPushService` shipped in shopifyLib since 2026-03-16 and is still unused by mppErsaPack today — see `ShopifyPushSync(BRIDGE)/AUDIT.md` drift log) |

## Prerequisites (status)
- ~~MercadoLibre developer account with API credentials~~ — ✅ satisfied (mppErsaPack has a live seller account, `userId=2622693752`, site MLM)
- ~~OAuth2 token refresh flow (ML tokens expire every 6 hours)~~ — ✅ satisfied by the consumer (mppErsaPack's `MercadoLibreApiClient.refreshAccessToken`, working in prod since 2026-05-13 per mppErsaPack LEDGER D42); this bridge does not reimplement refresh, it consumes a valid `accessToken` from `Marketplace.connectionConfig`
- ~~Decision on which ML API endpoints to support~~ — ✅ decided for this increment: `PUT /items/{id}` (stock only). Order/shipping/ads endpoints remain out of scope.

## Existing Code (before this bridge)
- `MercadoLibreProduct` model in mercadoLibreLib/commonMain
- `IMercadoLibreProductColl` abstract class with `fetchProducts()` hook in mercadoLibreLib/jvmMain
- `CommonMercadoLibreProduct` container in mercadoLibreLib/commonMain
- `syncToRemote()` stub returning "not supported" in `IMercadoLibreProductColl` — **unchanged by this bridge**, see SERVICE_CONTRACTS for why the new push service is a standalone primitive rather than wired through `syncToRemote()`

## New Code (this bridge)
- `IMercadoLibrePushService` (mercadoLibreLib/commonMain) — `updateAvailableQuantity(marketplace, itemId, quantity): SimpleState`
- `MercadoLibrePushEngine` (mercadoLibreLib/jvmMain) — Ktor-client (CIO) implementation, `PUT https://api.mercadolibre.com/items/{id}`
- `MercadoLibrePushEngineTest` (mercadoLibreLib/jvmTest) — 6 tests via `MockEngine`: success, 401, generic API error (with ML message), error JSON missing message/error fields, non-JSON error body (status-code preserved), missing-token fast-fail

---
[← Index](../INDEX.md) · **BRIEF** · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md) · [LEDGER](LEDGER.md)
