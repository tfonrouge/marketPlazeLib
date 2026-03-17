# MarketPlazeLib — API Surface

## Consumer Modules

| Consumer | Version | Integration |
|----------|---------|-------------|
| **mppErsaPack** | 1.1.0+ | Full: ShopifyCollections, RPC services, views, push sync |

## Core Models (commonLib)

```kotlin
data class Marketplace(_id, name, type, connectionConfig: Map<String,String>, enabled,
    ivaRate: Double, isrRate: Double, ivaIncluded: Boolean)

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
}

class SyncScheduler(scope) {
    fun schedule(SyncTask): String
    fun cancel(taskId) / cancelAll() / shutdown()
    fun isRunning(taskId): Boolean / activeTaskIds(): Set<String>
}
```

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
- `IErpProductReader` — ✅ commonLib/sync (4 methods; consumer implements)
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
