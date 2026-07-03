# Audit — ShopifyPushSync
**Last Audit**: 2026-07-03
**Audited By**: Claude Code assisted (cathedral-premise)
**Overall Status**: ⚠️ Partial Drift (library complete and functional; consumer wiring drifted — see 2026-07-03 entry)

## Drift Log (open, found 2026-07-03)
| Artifact | Section | Blueprint Said | Reality | Severity | Action |
|----------|---------|----------------|---------|----------|--------|
| BRIEF.md / IMPLEMENTATION_ORDER.md | Overall status | ✅ Complete, 22/22 tasks, implies the push path is live | `IShopifyPushService`/`ShopifyPushEngine` (10/10 methods) ship in shopifyLib but are **never referenced** by the consumer: `grep -rln "ShopifyPushEngine\|IShopifyPushService" mppErsaPack/ersaPack mppErsaPack/ersaPackLib` → zero matches (2026-07-03). mppErsaPack's real `pushInventoryToShopify()` (`MarketPlaceService.kt:32`) reimplements its own Apollo `InventorySetQuantitiesMutation` batching inline instead of calling `updateInventory()` | High | Not fixed in this audit (out of scope — this session's work was on `MercadoLibreIntegration(BRIDGE)`, not this bridge). Flagged so it isn't mistaken for "Complete and consumed." Fix options: (a) refactor mppErsaPack's `executePushSync` to call `IShopifyPushService` per-item instead of building its own batched mutation, accepting the per-item vs. batched-250 tradeoff; (b) extend `IShopifyPushService` with a batched `updateInventoryBulk()` matching what mppErsaPack actually needs, then wire it. Either requires a change in mppErsaPack, a separate repo/blueprint. |

## Drift Log (resolved 2026-05-01)
| Artifact | Section | Blueprint Said | Reality | Severity | Action Taken |
|----------|---------|----------------|---------|----------|--------------|
| SERVICE_CONTRACTS.md | image mutations (line 105) | `productCreateMedia` / `productDeleteMedia` | `productUpdate` (with `media` param) / `fileDelete` | High | Updated 2026-05-01 (v2.5.0 contract refresh) |
| SERVICE_CONTRACTS.md | `IErpProductReader` | 4 methods | 5 methods (`readInventoryByLocation` v2.3.0) | Medium | Updated 2026-05-01 |
| AUDIT.md | "Last Audit" | 2026-03-16 | Predates v2.3/v2.4/v2.5 work | High | Resolved by this re-run |

## Carry-forward (v1.1.0 ship — still aligned)
| Artifact | Section | Blueprint Says | Reality | Severity | Action |
|----------|---------|----------------|---------|----------|--------|
| IMPLEMENTATION_ORDER.md | Overall | 22/22 tasks complete | All layers shipped | None | Aligned |
| ENTITY_DESCRIPTOR.md | ShopifyCollection | Coll in B4-06 | `IShopifyCollectionColl` registered in `IShopifyCollections` | None | Aligned |
| SERVICE_CONTRACTS.md | IShopifyPushService | 10 methods | 10/10 functional | None | Aligned |

## Audit Checklist
- [x] Every entity in ENTITY_DESCRIPTOR.md has a corresponding implementation or explicit deferral
- [x] SERVICE_CONTRACTS.md matches current service signatures (interfaces + implementations) — refreshed for v2.5.0 mutation swap
- [x] VIEW_MAP.md — no new views added (bridge is backend-only; samples seed data updated)
- [x] IMPLEMENTATION_ORDER.md progress reflects actual completion
- [x] BRIEF.md scope still matches the planned feature
- [x] LEDGER.md created (2026-05-01) with falsification conditions for Permanent lifecycle and IErpProductReader contract
- [ ] **Real consumer confirmed calling `IShopifyPushService`** — FAILED 2026-07-03, see open Drift Log entry above. "22/22 tasks complete" tracked library-side work only; it did not track whether any consumer actually adopted the contract.

## Notes
- All 5 layers shipped on 2026-03-16 (v1.1.0)
- Layer 1: Product +12 fields, ProductVariant +5, ProductImage +position, ProductOption, PriceListEntry (pre-existing), ProductFilter +2, ShopifyCollection entity, SyncReport/SyncDiff/SyncError
- Layer 2: IMarketplaceSync.syncToRemote (abstract), IErpProductReader (commonLib), IShopifyPushService (shopifyLib)
- Layer 3: 4 GraphQL files — productMutations, publishableMutations, collections, mediaMutations (Shopify Admin 2025-10)
- Layer 4: ShopifyPushEngine (10/10 methods functional), ShopifyBidirectionalSyncEngine (reconcile + syncToRemote)
- Layer 5: ProductEnrichmentTest (6 tests), SyncEngineTest (7 tests), samples SeedData enriched
- `SimpleState` API: `hasError`/`msgOk`/`msgError` (not `isOk` as property)
- `buildApolloClient` duplicated in ShopifyPushEngine (standalone, not coupled to IShopifyColl)

### v2.5.0 update — Shopify mutation deprecations resolved
- `uploadImage` URL path: now uses `productUpdate` with `media` param (was `productCreateMedia`)
- `uploadImage` binary path: `stagedUploadsCreate` → HTTP PUT → `productUpdate` with `media`
- `deleteImage`: now uses `fileDelete` (was `productDeleteMedia`)
- `reorderImages`: still `productReorderMedia` (not deprecated)
- `IErpProductReader` extended with `readInventoryByLocation()` (default empty map; no breaking change to existing consumers)

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · [ORDER](IMPLEMENTATION_ORDER.md) · [LEDGER](LEDGER.md) · **AUDIT**
