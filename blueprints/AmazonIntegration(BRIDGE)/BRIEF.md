# AmazonIntegration — Bridge Brief

| Field | Value |
|-------|-------|
| Feature Name | `AmazonIntegration` |
| Business Problem | amazonLib defines `IAmazonProductColl` with an abstract `fetchProducts()` hook, but there is no real Amazon SP-API client implementation. Consumers must manually implement the HTTP calls to the Amazon Selling Partner API. This bridge provides a concrete API client that consumers can plug in. |
| Affected Modules | **amazonLib** (concrete API client for fetchProducts), **commonLib** (IMarketplaceSync — syncToRemote implementation), **consuming apps** (configure Amazon credentials in Marketplace.connectionConfig) |
| New Entities | None expected — `AmazonProduct` model already exists |
| Out of Scope | Amazon FBA integration, Amazon Advertising API, Amazon order management, Amazon A+ Content, Amazon Brand Registry |
| Owner | Teo Fonrouge |
| Target | TBD |
| **Library Version Target** | **MarketPlazeLib v1.2.0+** |
| **Lifecycle Type** | **Permanent** |

## Prerequisites
- Amazon Selling Partner API developer registration
- AWS IAM role for SP-API authentication (LWA + STS AssumeRole)
- Decision on which SP-API endpoints to support (Catalog Items, Listings, Pricing, Inventory)

## Existing Code
- `AmazonProduct` model (ASIN-based) in amazonLib/commonMain
- `IAmazonProductColl` abstract class with `fetchProducts()` hook in amazonLib/jvmMain
- `CommonAmazonProduct` container in amazonLib/commonMain
- `syncToRemote()` stub returning "not supported" in `IAmazonProductColl`

---
[← Index](../INDEX.md) · **BRIEF**
