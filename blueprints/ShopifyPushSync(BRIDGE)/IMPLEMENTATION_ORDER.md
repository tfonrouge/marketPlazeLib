# Implementation Order — ShopifyPushSync(BRIDGE)

**Updated**: 2026-03-16
**Status**: 22/22 tasks complete

## Checklist

### Layer 1 — Data Model Enrichment
- [x] B4-01 · Enrich `Product` model: add erpSourceId, priceLists, inventoryQuantity, descriptionHtml, publishEnabled, status, tags, weight, weightUnit, barcode, seoTitle, seoDescription
- [x] B4-02 · Add `PriceListEntry` value type (name, price, currencyCode, compareAtPrice)
- [x] B4-03 · Enrich `ProductVariant`: add compareAtPrice, barcode, weight, weightUnit, options
- [x] B4-04 · Enrich `ProductImage`: add position field
- [x] B4-05 · Add `ProductOption` value type (name, values)
- [x] B4-06 · Create `ShopifyCollection` entity (model + CommonContainer + filter; Coll deferred to B4-16 pending GraphQL query)
- [x] B4-07 · Create `SyncReport`, `SyncDiff`, `SyncError` value types
- [x] B4-08 · Update `ProductFilter` for new searchable fields

### Layer 2 — Interfaces
- [x] B4-09 · Add `syncToRemote(marketplace)` to `IMarketplaceSync` interface
- [x] B4-10 · Define `IErpProductReader` interface in commonLib
- [x] B4-11 · Define `IShopifyPushService` interface in shopifyLib

### Layer 3 — GraphQL Mutations
- [x] B4-12 · Write `productCreate.graphql` mutation
- [x] B4-13 · Write `productUpdate.graphql` + `productDelete.graphql` mutations
- [x] B4-14 · Write `inventorySetQuantities.graphql` mutation (already existed in inventory.graphql)
- [x] B4-15 · Write `publishablePublish.graphql` + `publishableUnpublish.graphql` mutations
- [x] B4-16 · Write `collectionAddProducts.graphql` + `collectionRemoveProducts.graphql` + `GetCollectionsQuery`
- [x] B4-17 · Write `stagedUploadsCreate.graphql` + `productCreateMedia.graphql` + `productDeleteMedia.graphql` + `productReorderMedia.graphql` mutations

### Layer 4 — Implementation
- [x] B4-18 · Implement `ShopifyPushEngine` (Apollo mutation execution)
- [x] B4-19 · Implement `ShopifyBidirectionalSyncEngine` (orchestration: reconcile + push)

### Layer 5 — Validation
- [x] B4-20 · Unit tests: Product enrichment serialization round-trip, backward compatibility, variant/image/option/filter tests
- [x] B4-21 · Update samples: enriched seed data with ERP fields, priceLists, tags, SEO, inventory
- [x] B4-22 · Integration test: SyncEngine reconcile + syncToRemote with fake ERP reader and push service (7 tests)

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · **ORDER** · [AUDIT](AUDIT.md)
