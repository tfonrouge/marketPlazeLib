# MarketPlazeLib — Implementation Plan

**Status: COMPLETED** — All phases shipped as v1.0.0

## Phase 1: Core Domain (commonLib) ✅
- **Deliverable**: Entity models, Money, value types, pricing services
- **Estimated Effort**: 2 dev-days
- **Owner**: Teo / Claude
- [x] Money value class with arithmetic operators
- [x] Value types: Commission, Fee, ShippingTier, AdvertisingRate, PublicationModality, FeeLineItem, ProductVariant, ProductImage
- [x] Entity models: Marketplace, Product, MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice
- [x] `data object` CommonContainers with `filterKClass` for each entity
- [x] Typed filter classes: MarketplaceFilter, ProductFilter, etc.
- [x] Abstract Coll classes (6 JVM collections, 4 type params: `<T, ID, FILT, UID>`)
- [x] MarketPlazeCollections factory
- [x] IMarketplaceSync interface
- [x] ProfitCalculator, MarketplaceComparator, PriceSuggester services
- [x] Delete: MarketPlaceId, IMarketProductId, IMarketProduct, IMarketProductColl

## Phase 2: shopifyLib Refactor ✅
- **Deliverable**: Shopify module adapted to new architecture
- **Estimated Effort**: 1 dev-day
- **Dependencies**: Phase 1
- [x] ShopifyProduct: storeId → marketplaceId
- [x] IShopifyColl: use Marketplace, implement IMarketplaceSync, 6 type params
- [x] ShopifyProductFilter with masterItemId: MarketplaceId?
- [x] CommonShopifyProduct as data object
- [x] IShopifyCollections extends MarketPlazeCollections
- [x] Delete: IStore, IStoreColl, ICommonStore

## Phase 3: Marketplace Modules ✅
- **Deliverable**: mercadoLibreLib + amazonLib modules
- **Estimated Effort**: 1 dev-day
- **Dependencies**: Phase 1
- [x] MercadoLibreProduct model, filter, CommonContainer, abstract Coll + IMarketplaceSync
- [x] AmazonProduct model, filter, CommonContainer, abstract Coll + IMarketplaceSync
- [x] Collection factory classes

## Phase 4: KVision View Building Blocks ✅
- **Deliverable**: Reusable column defs, filter views, form builders
- **Estimated Effort**: 1 dev-day
- **Dependencies**: Phase 1
- [x] 21 column definition functions (6 entities × 3-4 columns each)
- [x] 3 off-canvas filter view builders
- [x] 3 form field builders with formRow/formColumn layout

## Phase 5: SyncScheduler ✅
- **Deliverable**: Coroutine-based periodic sync scheduler
- **Estimated Effort**: 0.5 dev-day
- [x] SyncScheduler with schedule/cancel/cancelAll/shutdown lifecycle
- [x] 8 unit tests (schedule, cancel, reschedule, failure resilience)

## Phase 6: Samples Module ✅
- **Deliverable**: Runnable sample app with InMemoryRepository
- **Estimated Effort**: 1 dev-day
- **Dependencies**: All previous phases
- [x] RPC service interfaces with @RpcBindingRoute
- [x] SampleCollections with InMemoryRepository (no MongoDB)
- [x] SeedData with 3 marketplaces, 3 products, 5 listings, 2 cost structures, 4 competitor prices
- [x] 10 ViewList/ViewItem classes with companion ConfigView
- [x] App.kt with KVision + ViewRegistry + KVWebManager
- [x] Main.kt with Ktor server on port 8080
- [x] 14 integration tests

## Phase 7: Cleanup & Polish ✅
- **Deliverable**: Production-ready codebase
- **Estimated Effort**: 0.5 dev-day
- [x] Consolidated 6 CommonContainer files → CommonContainers.kt
- [x] Renamed ICommonShopifyProduct.kt → CommonShopifyProduct.kt
- [x] Deleted dead imageBanner.kt
- [x] Updated CLAUDE.md accuracy
- [x] Upgraded fsLib to 3.1.1 stable
- [x] All 37 tests passing, published v2.0.2

## Phase 8: Mexican Marketplace Pricing Engine ✅
- **Deliverable**: Accurate profit calculation for MX marketplaces (IVA/ISR/discount/shipping/health/per-piece)
- **Estimated Effort**: 1 dev-day
- **Dependencies**: Phase 1 (core models)
- [x] Marketplace: add ivaRate, isrRate, ivaIncluded
- [x] MarketplaceListing: add discount, listingUrl
- [x] ShippingTier: rename minWeight→minValue, maxWeight→maxValue, add tierType
- [x] Product: add presentations list
- [x] CostBreakdown: add finalPrice, taxableBase, ivaWithheld, isrWithheld, shippingCost, healthLevel, piecesCount, per-piece metrics
- [x] New value types: ProductPresentation, PriceListEntry, HealthLevel enum
- [x] resolveShippingCost() — price-based tier resolution
- [x] ProfitCalculator: discount, taxable base, IVA/ISR, shipping, health, per-piece
- [x] MarketplaceComparator: MarketplaceCostPair pattern
- [x] PriceSuggester: accounts for tax withholdings
- [x] PricingServiceTest: 10 tests (was 4) — MX taxes, discount, presentation, shipping, health
- [x] SampleIntegrationTest: updated for new MarketplaceComparator API
- [x] All 43 tests passing

## Phase 9: Remaining View Building Blocks
- [ ] colDefBreakdownHealthLevel(), colDefBreakdownIvaWithheld(), colDefBreakdownShippingCost(), colDefBreakdownFinalPrice()
- [ ] colDefMarketplaceIvaRate(), colDefMarketplaceIsrRate()
- [ ] colDefListingDiscount()
- [ ] Update marketplaceFormFields(): add ivaRate, isrRate, ivaIncluded
- [ ] Update productFormFields(): add presentations section

## Risk Register

| Risk | Impact | Mitigation | Status |
|------|--------|------------|--------|
| fsLib breaking changes | High — requires updating all generics | Pin fsLib version, adapt quickly when upgrading | Resolved (3.0.4→3.1.1 migration done) |
| Shopify GraphQL schema changes | Medium — @ShopifyField test catches mismatches | ShopifyTest validates at compile time via reflection | Mitigated |
| KVision view building blocks too thin | Low — consuming app can extend | Provide samples module as reference | Accepted |
| MercadoLibre/Amazon abstract implementations untested | Low — consumer responsibility | Document the pattern, provide fetchProducts() hook | Accepted |

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · **PLAN** · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [AUDIT](AUDIT.md)
