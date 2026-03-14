# MarketPlazeLib — API Contract

## commonLib Public API

### Models (commonLib/model)

```kotlin
// Money value class
value class Money(val cents: Long) : Comparable<Money>
  + plus, minus, times, div, unaryMinus, compareTo
  + Money.ZERO, Money.fromDecimal(units, decimals), toDecimal(decimals)

// Entities
data class Marketplace(_id, name, type, connectionConfig: Map<String,String>, enabled) : BaseDoc<MarketplaceId>
data class Product(_id, sku, name, baseCost: Money, variants, images) : BaseDoc<ProductId>
data class MarketplaceListing(_id, productId, marketplaceId, externalId, title, price: Money, publishEnabled) : BaseDoc<MarketplaceListingId>
data class CostStructure(_id, marketplaceId, name, commissions, fees, shippingTiers, advertisingRates, publicationModalities) : BaseDoc<CostStructureId>
data class CostBreakdown(_id, productId, marketplaceId, listingId?, salePrice, baseCost, lineItems, totalCosts, netProfit, profitMarginPercent) : BaseDoc<CostBreakdownId>
data class CompetitorPrice(_id, marketplaceId, productId?, competitorName, price: Money, observedAt: Instant) : BaseDoc<CompetitorPriceId>

// Value types
data class Commission(name, percentage: Double, fixedAmount: Money)
data class Fee(name, amount: Money, type: String)
data class ShippingTier(name, minWeight, maxWeight, cost: Money)
data class AdvertisingRate(name, costPerClick: Money, costPerImpression: Money)
data class PublicationModality(name, fee: Money, features: List<String>)
data class FeeLineItem(name, amount: Money, type: String)
data class ProductVariant(sku, name, price: Money, inventory: Int)
data class ProductImage(url, altText?, width?, height?)
```

### ICommonContainer classes (commonLib/common)

```kotlin
abstract class CommonMarketplace<FILT>(apiFilterSerializer) : ICommonContainer<Marketplace, MarketplaceId, FILT>
abstract class CommonProduct<FILT>(apiFilterSerializer) : ICommonContainer<Product, ProductId, FILT>
abstract class CommonMarketplaceListing<FILT>(apiFilterSerializer) : ICommonContainer<MarketplaceListing, MarketplaceListingId, FILT>
abstract class CommonCostStructure<FILT>(apiFilterSerializer) : ICommonContainer<CostStructure, CostStructureId, FILT>
abstract class CommonCostBreakdown<FILT>(apiFilterSerializer) : ICommonContainer<CostBreakdown, CostBreakdownId, FILT>
abstract class CommonCompetitorPrice<FILT>(apiFilterSerializer) : ICommonContainer<CompetitorPrice, CompetitorPriceId, FILT>
```

### Coll classes (commonLib/colls, JVM-only)

```kotlin
abstract class MarketplaceColl<CC, FILT, UID> : Coll<CC, Marketplace, MarketplaceId, FILT, UID>
abstract class ProductColl<CC, FILT, UID> : Coll<CC, Product, ProductId, FILT, UID>
abstract class MarketplaceListingColl<CC, FILT, UID> : Coll<CC, MarketplaceListing, MarketplaceListingId, FILT, UID>
abstract class CostStructureColl<CC, FILT, UID> : Coll<CC, CostStructure, CostStructureId, FILT, UID>
abstract class CostBreakdownColl<CC, FILT, UID> : Coll<CC, CostBreakdown, CostBreakdownId, FILT, UID>
abstract class CompetitorPriceColl<CC, FILT, UID> : Coll<CC, CompetitorPrice, CompetitorPriceId, FILT, UID>
```

### Services (commonLib/service)

```kotlin
object ProfitCalculator {
    fun calculate(salePrice: Money, baseCost: Money, costStructure: CostStructure, productId?, listingId?): CostBreakdown
}

object MarketplaceComparator {
    data class ComparisonResult(breakdowns, bestByProfit?, bestByMargin?)
    fun compare(salePrice: Money, baseCost: Money, costStructures: List<CostStructure>, productId?): ComparisonResult
}

object PriceSuggester {
    enum class PriceStrategy { MATCH_LOWEST, BELOW_AVERAGE, MATCH_AVERAGE }
    fun minimumPriceForMargin(baseCost: Money, costStructure: CostStructure, targetMarginPercent: Double): Money
    fun competitivePrice(competitorPrices: List<CompetitorPrice>, strategy: PriceStrategy): Money?
}
```

### Sync Interface (commonLib/sync)

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState
}
```

### Factory (commonLib/colls)

```kotlin
abstract class MarketPlazeCollections {
    abstract val MarketplaceColl, ProductColl, MarketplaceListingColl,
                 CostStructureColl, CostBreakdownColl, CompetitorPriceColl
}
```

## shopifyLib Public API

### Modified Classes

```kotlin
// ShopifyProduct — storeId replaced by marketplaceId
data class ShopifyProduct(_id: StringId, marketplaceId: MarketplaceId, handle, productType, publishedAt, title, description, descriptionHtml, vendor, totalInventory, media?)

// IShopifyColl — now implements IMarketplaceSync<T>
abstract class IShopifyColl<CC, T, ID, FILT, UID, QQ, D> : Coll<...>, IMarketplaceSync<T>
  fun buildApolloClient(marketplace: Marketplace): ApolloClient
  suspend fun syncShopifyToLocal(marketplace: Marketplace): SimpleState
  override suspend fun syncToLocal(marketplace: Marketplace): SimpleState

// IShopifyCollections — extends MarketPlazeCollections
abstract class IShopifyCollections<UID> : MarketPlazeCollections()
  abstract val ShopifyBusinessEntityColl, ShopifyProductColl
```

### Removed Classes
- `IStore`, `IStoreId`, `IStoreColl`, `ICommonStore` — replaced by `Marketplace` + `MarketplaceColl`
- `MarketPlaceId` enum — replaced by `Marketplace.type: String`
- `IMarketProductId`, `IMarketProduct`, `IMarketProductColl` — replaced by `Product` + `MarketplaceListing`
