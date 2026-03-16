# MarketPlazeLib — API Contract

## Consumer Modules

| Consumer | Version | Integration |
|----------|---------|-------------|
| **mppErsaPack** | 2.1.0+ | Full: ShopifyCollections, RPC services, views |

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
abstract class IShopifyCollections : MarketPlazeCollections()
```

## Error Handling

All sync operations return `SimpleState(isOk, msgOk/msgError)`.

## Versioning Policy

- Major (X.0.0): breaking API changes
- Minor (2.X.0): new features, backward compatible
- Patch (2.0.X): fixes, dependency upgrades
