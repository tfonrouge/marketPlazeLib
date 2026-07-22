# MarketPlazeLib — View Map

## View Inventory by Domain Area

### Marketplace Management
| View | Type | Status |
|------|------|--------|
| ViewListMarketplace | List (Tabulator) | Building block provided, app assembles |
| ViewItemMarketplace | Form (ViewFormPanel) | Building block provided, app assembles |

### Product Catalog
| View | Type | Status |
|------|------|--------|
| ViewListProduct | List (Tabulator) | Building block provided |
| ViewItemProduct | Form (ViewFormPanel) | Building block provided |

### Marketplace Listings
| View | Type | Status |
|------|------|--------|
| ViewListMarketplaceListing | List (Tabulator) | Building block provided |

### Cost Analysis
| View | Type | Status |
|------|------|--------|
| ViewListCostStructure | List (Tabulator) | Building block provided |
| ViewListCostBreakdown | List (Tabulator) | Building block provided (read-only) |

### Competitor Intelligence
| View | Type | Status |
|------|------|--------|
| ViewListCompetitorPrice | List (Tabulator) | Building block provided |
| ViewItemCompetitorPrice | Form (ViewFormPanel) | Building block provided |

### Shopify
| View | Type | Status |
|------|------|--------|
| ViewListShopifyProduct | List (Tabulator) | Building block provided |

## Library vs App Responsibility

The library provides **reusable building blocks** — the consuming app creates the actual ViewList/ViewItem classes because they require a no-arg constructor referencing a global `configViewList`/`configViewItem` (fsLib constraint).

### What the library provides (commonLib/jsMain)

**Column definitions** (`view/colDef/`):
- `colDefMarketplaceName()`, `colDefMarketplaceType()`, `colDefMarketplaceEnabled()`
- `colDefProductSku()`, `colDefProductName()`, `colDefProductBaseCost()`, `colDefProductVariantCount()`
- `colDefListingExternalId()`, `colDefListingTitle()`, `colDefListingPrice()`, `colDefListingPublishEnabled()`
- `colDefCostStructureName()`, `colDefCostStructureCommissionCount()`, `colDefCostStructureFeeCount()`
- `colDefBreakdownSalePrice()`, `colDefBreakdownTotalCosts()`, `colDefBreakdownNetProfit()`, `colDefBreakdownProfitMargin()`
- `colDefCompetitorName()`, `colDefCompetitorPriceAmount()`, `colDefCompetitorObservedAt()`

**Filter views** (`view/filter/`):
- `offCanvasMarketplaceFilter()` — type text + enabled radio
- `bannerLegendMarketplaceFilter()` — active filter summary
- `offCanvasListingFilter()` — published radio
- `offCanvasCompetitorPriceFilter()` — competitor name text

**Form builders** (`view/form/`):
- `marketplaceFormFields()` — name (6col), type (3col), enabled (3col)
- `productFormFields()` — sku (3col), name (9col)
- `competitorPriceFormFields()` — competitorName (12col)

### What the consuming app provides

```kotlin
// App creates ViewList with companion configViewList (fsLib showcase pattern)
class ViewListMarketplace : ViewList<Marketplace, MarketplaceId, MarketplaceFilter, Unit>(
    configView = configViewList,
) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListMarketplace::class,
            commonContainer = CommonMarketplace,
            apiListFun = IMyListService::marketplace,  // app's RPC function
        )
    }
    override fun columnDefinitionList() = listOf(
        colDefMarketplaceName(), colDefMarketplaceType(), colDefMarketplaceEnabled()
    )
    override fun Container.buildOffCanvasFilterView() = offCanvasMarketplaceFilter(this)
    override fun Container.pageListBody() { fsTabulator(this@ViewListMarketplace) }
}
```

## Samples Module Views (complete working examples)

The `samples` module provides 10 fully assembled ViewList/ViewItem classes as reference implementations. Run with `./gradlew :samples:run`.

## Navigation

Navigation handled by fsLib's `ViewRegistry` + `KVWebManager`:
- `ConfigViewList.openView()` → navigates to list view
- `ConfigViewItem.navigateToQueryRead(id)` → navigates to item detail
- App registers service managers: `ViewRegistry.listServiceManager`, `ViewRegistry.itemServiceManager`

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · **VIEWS** · [PLAN](IMPLEMENTATION_PLAN.md) · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [AUDIT](AUDIT.md)
