# ShopifyPushSync — Bridge Brief

| Field | Value |
|-------|-------|
| Feature Name | `ShopifyPushSync` |
| Business Problem | marketPlazeLib can only pull data FROM Shopify but cannot push product data TO Shopify. ERP systems (mppErsaPack/SAE) have inventory, prices, and descriptions that need to be published to Shopify stores — currently done manually. |
| Affected Modules | **commonLib** (Product model enrichment, IMarketplaceSync interface), **shopifyLib** (GraphQL mutations, push engine, ShopifyCollection), **consuming apps** (implement IErpProductReader) |
| New Entities | `ShopifyCollection` (1 entity) |
| Out of Scope | MercadoLibre push sync, Amazon push sync, Shopify webhook/event listeners, Shopify order management, Shopify discount/coupon management, multi-currency price list management beyond Shopify's built-in, ERP-specific data mapping (that's the consumer's job via IErpProductReader) |
| Owner | Teo Fonrouge |
| Target | Shipped 2026-03-16 |
| **Library Version Target** | **MarketPlazeLib v1.1.0** (breaking: adds `syncToRemote()` to `IMarketplaceSync`) |
| **Lifecycle Type** | **Permanent** |

---
[← Index](../INDEX.md) · **BRIEF** · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md)
