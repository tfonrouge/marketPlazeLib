# Implementation Order — ShopifyPushSync(BRIDGE)

**Updated**: 2026-03-15
**Status**: 0/20 tasks complete

## Checklist

### Layer 1 — Data Model Enrichment
- [ ] B4-01 · Enrich `Product` model: add erpSourceId, priceLists, inventoryQuantity, descriptionHtml, publishEnabled, status, tags, weight, weightUnit, barcode, seoTitle, seoDescription
- [ ] B4-02 · Add `PriceListEntry` value type (name, price, currencyCode, compareAtPrice)
- [ ] B4-03 · Enrich `ProductVariant`: add compareAtPrice, barcode, weight, weightUnit, options
- [ ] B4-04 · Enrich `ProductImage`: add position field
- [ ] B4-05 · Add `ProductOption` value type (name, values)
- [ ] B4-06 · Create `ShopifyCollection` entity (model + CommonContainer + Coll + filter)
- [ ] B4-07 · Create `SyncReport` and `SyncDiff` value types
- [ ] B4-08 · Update `ProductFilter` for new searchable fields

### Layer 2 — Interfaces
- [ ] B4-09 · Add `syncToRemote(marketplace)` to `IMarketplaceSync` interface
- [ ] B4-10 · Define `IErpProductReader` interface in commonLib
- [ ] B4-11 · Define `IShopifyPushService` interface in shopifyLib

### Layer 3 — GraphQL Mutations
- [ ] B4-12 · Write `productCreate.graphql` mutation
- [ ] B4-13 · Write `productUpdate.graphql` + `productDelete.graphql` mutations
- [ ] B4-14 · Write `inventorySetQuantities.graphql` mutation
- [ ] B4-15 · Write `publishablePublish.graphql` + `publishableUnpublish.graphql` mutations
- [ ] B4-16 · Write `collectionAddProducts.graphql` + `collectionRemoveProducts.graphql` + `GetCollectionsQuery`
- [ ] B4-17 · Write `stagedUploadsCreate.graphql` + `productCreateMedia.graphql` + `productDeleteMedia.graphql` + `productReorderMedia.graphql` mutations

### Layer 4 — Implementation
- [ ] B4-18 · Implement `ShopifyPushEngine` (Apollo mutation execution)
- [ ] B4-19 · Implement `ShopifyBidirectionalSyncEngine` (orchestration: reconcile + push + pull)

### Layer 5 — Validation
- [ ] B4-20 · Unit tests: Product enrichment serialization, SyncReport creation, reconcile logic
- [ ] B4-21 · Update samples: enriched seed data, extended productFormFields, ShopifyCollection views
- [ ] B4-22 · Integration test: mock Shopify API responses for create/update/delete cycle
