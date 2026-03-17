# MercadoLibreIntegration — Bridge Brief

| Field | Value |
|-------|-------|
| Feature Name | `MercadoLibreIntegration` |
| Business Problem | mercadoLibreLib defines `IMercadoLibreProductColl` with an abstract `fetchProducts()` hook, but there is no real MercadoLibre API client implementation. Consumers must manually implement the HTTP calls to the MercadoLibre API. This bridge provides a concrete API client that consumers can plug in. |
| Affected Modules | **mercadoLibreLib** (concrete API client for fetchProducts), **commonLib** (IMarketplaceSync — syncToRemote implementation), **consuming apps** (configure ML credentials in Marketplace.connectionConfig) |
| New Entities | None expected — `MercadoLibreProduct` model already exists |
| Out of Scope | MercadoLibre order management, MercadoLibre shipping integration, MercadoLibre advertising API, MercadoLibre webhook listeners |
| Owner | Teo Fonrouge |
| Target | TBD |
| **Library Version Target** | **MarketPlazeLib v1.2.0+** |
| **Lifecycle Type** | **Permanent** |

## Prerequisites
- MercadoLibre developer account with API credentials
- OAuth2 token refresh flow (ML tokens expire every 6 hours)
- Decision on which ML API endpoints to support (items, questions, orders)

## Existing Code
- `MercadoLibreProduct` model in mercadoLibreLib/commonMain
- `IMercadoLibreProductColl` abstract class with `fetchProducts()` hook in mercadoLibreLib/jvmMain
- `CommonMercadoLibreProduct` container in mercadoLibreLib/commonMain
- `syncToRemote()` stub returning "not supported" in `IMercadoLibreProductColl`

---
[← Index](../INDEX.md) · **BRIEF**
