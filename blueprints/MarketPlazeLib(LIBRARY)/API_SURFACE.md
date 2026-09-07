# MarketPlazeLib — API Surface

## Consumer Modules

| Consumer | Version | Integration |
|----------|---------|-------------|
| **mppErsaPack** | 1.1.0+ | Full: ShopifyCollections, RPC services, views, push sync |
| **mppDulce** | 2.11.10 (2.11.14 pending) | `shopifyLib` only — Tiendas (BRIDGE) WinCaja + Tiendas ↔ marketPlazeLib |

## Core Models (commonLib)

```kotlin
object MarketplaceType {                                   // v2.5.0
    const val MERCADO_LIBRE = "MercadoLibre"
    const val SHOPIFY = "Shopify"
    const val AMAZON = "Amazon"
}

data class Marketplace(_id, name, type, connectionConfig: Map<String,String>, enabled,
    ivaRate: Double, isrRate: Double, ivaIncluded: Boolean,
    commissionRateClassic: Double, commissionRatePremium: Double,    // v1.2.0/2.1.0 (Mexican pricing)
    cargoFijoTiers: List<CargoFijoTier>,                             // v1.2.0/2.1.0
    envioTiers: List<EnvioTier>, envioDiscounts: List<EnvioDiscount>,// v1.2.0/2.1.0
    isDefault: Boolean, minimumPrice: Money)

data class Product(_id, sku, name, baseCost: Money, variants, images,
    presentations: List<ProductPresentation>)

data class MarketplaceListing(_id, productId↗, marketplaceId↗, externalId, title,
    price: Money, publishEnabled, discount: Double, listingUrl: String?)

data class CostStructure(_id, marketplaceId↗, name, commissions, fees,
    shippingTiers, advertisingRates, publicationModalities)

data class CostBreakdown(_id, productId↗, marketplaceId↗, listingId↗?,
    salePrice, finalPrice, baseCost, taxableBase, ivaWithheld, isrWithheld,
    shippingCost, lineItems, totalCosts, netProfit, profitMarginPercent,
    healthLevel: String, piecesCount: Int?, pricePerPiece?, costPerPiece?, profitPerPiece?)

data class CompetitorPrice(_id, marketplaceId↗, productId↗?, competitorName, price, observedAt)
```

## Value Types

```kotlin
value class Money(val cents: Long) : Comparable<Money>
data class Commission(name, percentage: Double, fixedAmount: Money)
data class Fee(name, amount: Money, type: String)
data class ShippingTier(name, minValue: Double, maxValue: Double, tierType: String, cost: Money)
data class ProductVariant(sku, name, price: Money, inventory: Int)
data class ProductImage(url, altText?, width?, height?)
data class ProductPresentation(label, piecesPerPackage, packagesPerDisplay, packagingCost, isHypothetical)
data class PriceListEntry(name, price: Money, currencyCode, compareAtPrice?)
data class FeeLineItem(name, amount: Money, type: String)
enum class HealthLevel { CRITICAL, ACCEPTABLE, HEALTHY, EXCELLENT; classify(margin%) }
```

## Pricing Services

```kotlin
object ProfitCalculator {
    fun calculate(
        salePrice: Money, baseCost: Money, costStructure: CostStructure,
        marketplace: Marketplace = default,  // for ivaRate, isrRate, ivaIncluded
        discount: Double = 0.0,              // listing discount
        presentation: ProductPresentation? = null,  // per-piece metrics
        productId: ProductId = OId(), listingId: MarketplaceListingId? = null,
        shippingCostOverride: Money? = null,
    ): CostBreakdown
}

fun resolveShippingCost(price: Money, tiers: List<ShippingTier>): Money

object MarketplaceComparator {
    data class MarketplaceCostPair(marketplace: Marketplace, costStructure: CostStructure)
    data class ComparisonResult(breakdowns, bestByProfit?, bestByMargin?)
    fun compare(salePrice, baseCost, pairs: List<MarketplaceCostPair>,
        discount, presentation?, productId?): ComparisonResult
}

object PriceSuggester {
    fun minimumPriceForMargin(baseCost, costStructure, marketplace?, targetMarginPercent): Money
    fun competitivePrice(competitorPrices, strategy): Money?
}
```

## Sync Interface

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState
    suspend fun syncToRemote(marketplace: Marketplace): SimpleState  // v1.1.0
}

interface IErpProductReader {                                         // v1.1.0
    suspend fun readProducts(): List<Product>
    suspend fun readProductById(erpId: String): Product?
    suspend fun readInventoryLevels(): Map<String, Int>
    suspend fun readPriceLists(): Map<String, List<PriceListEntry>>
    suspend fun readInventoryByLocation(): Map<String, Map<String, Int>> = emptyMap()  // v2.3.0
}

class SyncScheduler(scope) {
    fun schedule(SyncTask): String
    fun cancel(taskId) / cancelAll() / shutdown()
    fun isRunning(taskId): Boolean / activeTaskIds(): Set<String>
}
```

### Sync Persistence Ports (commonLib/sync) — v2.11.12, hardened v2.11.13–v2.11.14

> Where listings, run history and incremental cursors live between syncs. The library reads and
> writes them but never decides the store: the consumer implements the port (Mongo, Postgres…) and
> must satisfy the same assertions as `SyncStoreContractTest` (commonLib:jvmTest, 17 test cases) — if
> it diverges from the in-memory reference, the consumer's implementation is the one that is wrong.
> Decisions: LEDGER 2026-09-06 (ports; no mutual exclusion / kill switch in this batch — both are
> prerequisites before automating any write; `_id` preservation; `SyncRun` snapshot).

```kotlin
interface IListingStore {                                              // v2.11.12
    suspend fun listings(marketplaceId↗): List<MarketplaceListing>     // empty list, never null
    suspend fun byExternalId(marketplaceId↗, externalId: String): MarketplaceListing?
    suspend fun upsert(listing: MarketplaceListing)                    // keyed by (marketplaceId, externalId), idempotent;
                                                                       // on replace KEEPS the stored _id (v2.11.13); delete frees the key
    suspend fun upsertAll(listings: List<MarketplaceListing>)          // default: N × upsert; batch, not transactional unless the impl says so
    suspend fun count(marketplaceId↗): Int                             // default: listings(m).size
    suspend fun delete(marketplaceId↗, externalId: String)             // per item only — no bulk delete on purpose
}

interface ISyncRunStore {                                              // v2.11.12
    suspend fun record(run: SyncRun)                                   // stores as given, never interprets
    suspend fun last(marketplaceId↗, kind: SyncKind): SyncRun?         // most recent by finishedAt, not last inserted
    suspend fun history(marketplaceId↗, kind: SyncKind? = null, limit: Int = 50): List<SyncRun>  // newest first
}

interface ISyncCheckpointStore {                                       // v2.11.12
    suspend fun get(marketplaceId↗, key: String): String?              // null = "start from the beginning"; "" is a stored value
    suspend fun set(marketplaceId↗, key: String, value: String)        // opaque value; key must not embed the marketplace
    suspend fun clear(marketplaceId↗, key: String)                     // forces the next pass to be full
}

enum class SyncKind { RECONCILE, PUSH_PRODUCTS, PUSH_INVENTORY, PUSH_MEDIA, COLLECTIONS }
enum class SyncOutcome { OK, PARTIAL, FAILED, SKIPPED }

@Serializable data class SyncRunError(item: String? = null, message: String)   // text, not a counter

@Serializable @ConsistentCopyVisibility
data class SyncRun private constructor(marketplaceId↗, kind: SyncKind, outcome: SyncOutcome,
    finishedAt: Long /* epoch ms UTC */, durationMillis: Long = 0, examined: Int = 0, changed: Int = 0,
    errors: List<SyncRunError> = emptyList(), skippedReason: String? = null) {
    companion object { operator fun invoke(/* same parameters and defaults */): SyncRun }
    // Public entry is SyncRun(...) via invoke — same call syntax. errors is a read-only snapshot: independent of
    // the input list (v2.11.13) and not mutable through the getter or after deserialization (v2.11.14).
    // init: SKIPPED requires skippedReason; OK rejects errors (that case is PARTIAL). copy is private (v2.11.13).
}

// Reference implementations — tests and samples only; not concurrency-safe; lose everything on restart
class InMemoryListingStore : IListingStore
class InMemorySyncRunStore : ISyncRunStore
class InMemorySyncCheckpointStore : ISyncCheckpointStore
```

Compatibility note: v2.11.13 removed `SyncRun`'s public constructor and `copy` one release after
they appeared; no consumer had adopted `SyncRun` (mppErsaPack on 2.11.11, mppDulce on 2.11.10), so
it shipped as a patch.

## Shopify API (shopifyLib)

```kotlin
abstract class IShopifyColl<T, ID, FILT, UID, QQ, D> : Coll<T, ID, FILT, UID>, IMarketplaceSync<T>
abstract class IShopifyCollections : MarketPlazeCollections()  // includes ShopifyCollectionColl

interface IShopifyPushService {                                       // v1.1.0
    suspend fun createProduct(marketplace, product): SimpleState
    suspend fun updateProduct(marketplace, product, shopifyId): SimpleState
    suspend fun deleteProduct(marketplace, shopifyId): SimpleState
    suspend fun updateInventory(marketplace, shopifyId, locationId, quantity): SimpleState
    suspend fun setPublishState(marketplace, shopifyId, published): SimpleState
    suspend fun addToCollection(marketplace, collectionId, shopifyProductId): SimpleState
    suspend fun removeFromCollection(marketplace, collectionId, shopifyProductId): SimpleState
    suspend fun uploadImage(marketplace, shopifyId, imageUrl, altText): SimpleState
    suspend fun deleteImage(marketplace, shopifyId, imageId): SimpleState
    suspend fun reorderImages(marketplace, shopifyId, imageIds): SimpleState
}
```

## Error Handling

All sync operations return `SimpleState(isOk, msgOk/msgError)`.

## v1.1.0 API Changes (ShopifyPushSync) — Complete
> See [blueprints/ShopifyPushSync(BRIDGE)/](../ShopifyPushSync(BRIDGE)/BRIEF.md) — 22/22 tasks complete

- `IMarketplaceSync.syncToRemote(marketplace)` — ✅ abstract; stubs in MercadoLibre/Amazon
- `IErpProductReader` — ✅ commonLib/sync (5 methods as of v2.3.0; consumer implements; `readInventoryByLocation` defaults to empty map)
- `IShopifyPushService` — ✅ shopifyLib (10 methods)
- `Product` — ✅ enriched with 12 optional fields
- `ShopifyCollection` — ✅ model + filter + CommonContainer + Coll + registered in IShopifyCollections
- `SyncReport`, `SyncDiff`, `SyncError` — ✅ shopifyLib
- GraphQL mutations — ✅ 4 files, Apollo-validated against Shopify Admin 2025-10
- `ShopifyPushEngine` — ✅ IShopifyPushService impl (10/10 functional)
- `ShopifyBidirectionalSyncEngine` — ✅ reconcile() + syncToRemote()
- Tests — ✅ 13 new tests (6 ProductEnrichment + 7 SyncEngine)
- Samples — ✅ seed data enriched with ERP fields

## Versioning Policy

- Major (X.0.0): breaking API changes
- Minor (2.X.0): new features, backward compatible
- Patch (2.0.X): fixes, dependency upgrades

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · **API SURFACE** · [VIEWS](VIEW_MAP.md) · [PLAN](IMPLEMENTATION_PLAN.md) · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [AUDIT](AUDIT.md)
