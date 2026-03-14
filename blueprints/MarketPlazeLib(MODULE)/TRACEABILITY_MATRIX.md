# MarketPlazeLib — Traceability Matrix

## Requirements → Implementation

| Requirement | Source File(s) | Test |
|-------------|---------------|------|
| Money as Long (cents) value class | `commonLib/.../model/Money.kt` | `MoneyTest` |
| Marketplace entity replaces MarketPlaceId enum + IStore | `commonLib/.../model/Marketplace.kt` | — |
| Product entity with SKU and base cost | `commonLib/.../model/Product.kt` | — |
| MarketplaceListing replaces IMarketProduct | `commonLib/.../model/MarketplaceListing.kt` | — |
| CostStructure per marketplace | `commonLib/.../model/CostStructure.kt` | `PricingServiceTest` |
| CostBreakdown calculated result | `commonLib/.../model/CostBreakdown.kt` | `PricingServiceTest` |
| CompetitorPrice tracking | `commonLib/.../model/CompetitorPrice.kt` | `PricingServiceTest` |
| ProfitCalculator service | `commonLib/.../service/ProfitCalculator.kt` | `PricingServiceTest` |
| MarketplaceComparator service | `commonLib/.../service/MarketplaceComparator.kt` | `PricingServiceTest` |
| PriceSuggester service | `commonLib/.../service/PriceSuggester.kt` | `PricingServiceTest` |
| IMarketplaceSync interface | `commonLib/.../sync/IMarketplaceSync.kt` | — |
| ICommonContainer per entity (×6) | `commonLib/.../common/Common*.kt` | — |
| Abstract Coll per entity (×6) | `commonLib/.../colls/*Coll.kt` | — |
| MarketPlazeCollections factory | `commonLib/.../colls/MarketPlazeCollections.kt` | — |
| ShopifyProduct uses marketplaceId | `shopifyLib/.../model/ShopifyProduct.kt` | `ShopifyTest` |
| IShopifyColl implements IMarketplaceSync | `shopifyLib/.../colls/IShopifyColl.kt` | — |
| IShopifyCollections extends MarketPlazeCollections | `shopifyLib/.../colls/IShopifyCollections.kt` | — |

## Deleted → Replaced By

| Deleted | Replaced By |
|---------|-------------|
| `MarketPlaceId` enum | `Marketplace.type: String` |
| `IMarketProductId<PID>` | `MarketplaceListing.externalId` |
| `IMarketProduct<ID>` | `Product` + `MarketplaceListing` |
| `IMarketProductColl` | `ProductColl` + `MarketplaceListingColl` |
| `IStore` | `Marketplace` with `connectionConfig` |
| `IStoreColl` | `MarketplaceColl` |
| `ICommonStore` | `CommonMarketplace` |
