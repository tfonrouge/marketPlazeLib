# MarketPlazeLib — Project Brief

## Vision
Transform marketPlazeLib from a Shopify-only sync library into a marketplace intelligence ecosystem supporting multi-marketplace product management, cost analysis, and competitive pricing.

## Scope
- **commonLib**: Core domain models (Marketplace, Product, MarketplaceListing, CostStructure, CostBreakdown, CompetitorPrice), Money value class, pricing services (ProfitCalculator, MarketplaceComparator, PriceSuggester), IMarketplaceSync interface, abstract Coll classes, MarketPlazeCollections factory
- **shopifyLib**: Shopify-specific implementation using Apollo GraphQL, refactored to use Marketplace entity instead of IStore, implements IMarketplaceSync

## Key Decisions
- **Money**: `Long` (cents) value class — exact arithmetic, KMP-safe
- **Modules**: 2 (commonLib + shopifyLib) — YAGNI, extract when needed
- **Connection config**: Generic `Map<String, String>` per marketplace
- **No backward compatibility**: Breaking change (v2.0.0)

## Architecture
```
commonLib (models, services, abstractions)
    ↑
shopifyLib (Shopify GraphQL sync, @ShopifyField)
    ↑
futuro: mercadoLibreLib, amazonLib (each implements IMarketplaceSync)
```
