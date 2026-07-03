# MercadoLibreIntegration — Service Contracts

## Contract Summary

| Direction | Caller → Callee | Method | Purpose |
|-----------|----------------|--------|---------|
| Library → MercadoLibre | `IMercadoLibrePushService` → MercadoLibre API | `updateAvailableQuantity()` | Set an item's stock on MercadoLibre |

## IMercadoLibrePushService — Library → MercadoLibre API

**Direction**: mercadoLibreLib implements; uses Ktor client (CIO engine) against MercadoLibre's REST API.
**Owner**: mercadoLibreLib module

```kotlin
interface IMercadoLibrePushService {
    suspend fun updateAvailableQuantity(marketplace: Marketplace, itemId: String, quantity: Int): SimpleState
}
```

### updateAvailableQuantity(marketplace, itemId, quantity)
- **Trigger**: Caller determined that MercadoLibre's published stock for `itemId` no longer matches the true source-of-truth quantity (e.g. ERP inventory changed) and wants to correct it
- **Input**: `Marketplace` (must have `connectionConfig["accessToken"]`), `itemId: String` (MercadoLibre item id, e.g. `MLM2801064685`), `quantity: Int` (new `available_quantity`)
- **Output**: `SimpleState` — `hasError=false` + `msgOk` on success; `hasError=true` + `msgError` on failure (missing token / 401 / other API error / network exception)
- **Side Effects**: Updates the live MercadoLibre listing's stock. No local persistence — this bridge does not write to any `Coll`/MongoDB.
- **REST call**: `PUT https://api.mercadolibre.com/items/{itemId}` with body `{"available_quantity": <quantity>}`, header `Authorization: Bearer <accessToken>`
- **Does NOT**: refresh an expired token (401 is surfaced, not retried), batch multiple items in one call (no such ML endpoint exists), resolve `itemId` from a SKU/Clave (caller's responsibility)

## Why this is a standalone primitive, not wired through `IMercadoLibreProductColl.syncToRemote()`

`IMercadoLibreProductColl.syncToRemote()` (existing, unchanged by this bridge) is a generic `Coll`-level hook with no way to receive an externally computed quantity (e.g. from an ERP) — it operates on whatever is already persisted locally in the `MercadoLibreProduct` collection, which itself is populated by *pulling* from MercadoLibre, not from an ERP. Wiring push through it would require the Coll to also know how to read ERP stock, which breaks the module's dependency direction (mercadoLibreLib must not depend on any specific ERP's data model).

This mirrors the existing precedent in shopifyLib: `IShopifyPushService.updateInventory()` is also a standalone primitive, invoked directly by app-side orchestration (`ShopifyBidirectionalSyncEngine` in the library, or ad hoc in the consumer), not threaded through `IShopifyColl.syncToRemote()` either.

## Events

No domain events emitted. Synchronous request/response only, matching the `IShopifyPushService` precedent.

## Shared Entities

| Entity | Shared Between | Notes |
|--------|---------------|-------|
| `Marketplace` | commonLib ↔ mercadoLibreLib | `connectionConfig["accessToken"]` provides MercadoLibre bearer credentials |

## Known Limitation — Caller-Owned Token Refresh

Unlike ShopifyPushEngine (which needs no refresh — Shopify uses long-lived API access tokens), MercadoLibre access tokens expire every 6 hours. This engine does **not** implement the refresh dance (PKCE, `refresh_token` rotation) — that logic already exists and is proven in prod on the consumer side (mppErsaPack's `MercadoLibreApiClient.refreshAccessToken`, persists rotated tokens to its own MongoDB). Duplicating it here would either (a) require this library to own MongoDB persistence of `Marketplace.connectionConfig`, which no other part of mercadoLibreLib does, or (b) leave a second, unsynchronized copy of refresh logic. Both are worse than the current contract: **on 401, the caller refreshes (via its own existing mechanism) and retries** `updateAvailableQuantity` with the new token.

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · **CONTRACTS** · [VIEWS](VIEW_MAP.md) · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md) · [LEDGER](LEDGER.md)
