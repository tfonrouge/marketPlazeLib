# MarketPlazeLib — View Map

## JS Components (KVision)

### Existing
- `shopifyLib/jsMain/view/imageBanner/imageBanner.kt` — Image banner component (unchanged)

### Future Views (to be implemented by consuming projects)

| View | Module | Purpose |
|------|--------|---------|
| Marketplace List | commonLib/jsMain | CRUD for registered marketplaces |
| Product Catalog | commonLib/jsMain | Product management with variants/images |
| Listing Manager | commonLib/jsMain | Per-marketplace listing status |
| Cost Structure Editor | commonLib/jsMain | Commission/fee/tier configuration |
| Profit Dashboard | commonLib/jsMain | CostBreakdown visualization |
| Competitor Price Tracker | commonLib/jsMain | Price monitoring grid |
| Marketplace Comparison | commonLib/jsMain | Side-by-side profit comparison |

### Data Flow

```
User → KVision Component → Kilua RPC → JVM Service → MongoDB Coll
                                         ↓
                              ProfitCalculator / MarketplaceComparator
                                         ↓
                                    CostBreakdown
```
