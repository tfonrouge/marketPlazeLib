# MarketPlazeLib — Technical Specification

## Domain Model

### Entities (BaseDoc + ICommonContainer + Coll)

| Entity | ID Type | Package | Purpose |
|--------|---------|---------|---------|
| `Marketplace` | `OId<Marketplace>` | commonLib | Registered marketplace with connection config |
| `Product` | `OId<Product>` | commonLib | Own product with SKU and base cost |
| `MarketplaceListing` | `OId<MarketplaceListing>` | commonLib | Product published on a marketplace |
| `CostStructure` | `OId<CostStructure>` | commonLib | Fee/commission structure per marketplace |
| `CostBreakdown` | `OId<CostBreakdown>` | commonLib | Calculated cost breakdown per product×marketplace |
| `CompetitorPrice` | `OId<CompetitorPrice>` | commonLib | Observed competitor price |

### Value Types (no collection)

| Type | Purpose |
|------|---------|
| `Money` | Value class wrapping `Long` (cents) with arithmetic operators |
| `Commission` | Named percentage + fixed amount |
| `Fee` | Named amount with type (fixed/percentage/per_unit) |
| `ShippingTier` | Weight-based shipping cost tier |
| `AdvertisingRate` | CPC/CPM advertising costs |
| `PublicationModality` | Publication mode with fee and features |
| `FeeLineItem` | Calculated fee line in a breakdown |
| `ProductVariant` | SKU variant with price and inventory |
| `ProductImage` | Image URL with metadata |

## Pricing Services (commonLib, pure functions, KMP-safe)

### ProfitCalculator
`calculate(salePrice, baseCost, costStructure) → CostBreakdown`
Applies commissions, fees, and modality fees to produce a complete cost breakdown.

### MarketplaceComparator
`compare(salePrice, baseCost, costStructures[]) → ComparisonResult`
Ranks marketplaces by net profit and profit margin.

### PriceSuggester
- `minimumPriceForMargin(baseCost, costStructure, targetMargin%) → Money`
- `competitivePrice(competitorPrices[], strategy) → Money?`

## Sync Interface

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState
}
```

Implemented by `IShopifyColl` in shopifyLib. Future marketplace modules implement this interface.

## Marketplace Connection

`Marketplace.connectionConfig: Map<String, String>` stores credentials generically.
For Shopify: `{"url": "...", "apiVersion": "...", "accessToken": "..."}`.

## Collection Factory

```kotlin
abstract class MarketPlazeCollections {
    abstract val MarketplaceColl, ProductColl, MarketplaceListingColl,
                 CostStructureColl, CostBreakdownColl, CompetitorPriceColl
}

abstract class IShopifyCollections<UID> : MarketPlazeCollections() {
    abstract val ShopifyBusinessEntityColl, ShopifyProductColl
}
```
