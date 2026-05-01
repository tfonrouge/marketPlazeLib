# MercadoLibreOrders &mdash; Feature Brief

| Field | Value |
|-------|-------|
| Feature Name | `MercadoLibreOrders` |
| Mode | FEATURE |
| Business Owner | Teo Fonrouge |
| Business Justification | Shared order models for MercadoLibre, reusable by any app consuming marketPlazeLib. Orders are the next data domain after products &mdash; apps need to pull ML orders, store them locally, and display them. Centralizing the model + sync logic in the library avoids duplication across consumers. |
| Affected Module | **mercadoLibreLib** (new model + filter + common + abstract coll) |
| Platform/Runtime Targets | KMP &mdash; JVM + JS (ES2015, IR compiler) |
| Target | TBD (current ≥ v2.5.0); model + abstract coll already shipped in commit `7df6b3f` |
| Lifecycle Type | Permanent |
| Status | 🟡 In Progress — TEST_PLAN and AUDIT pending |

## Scope

- **commonMain** &mdash; `MercadoLibreOrder` data class with embedded types (`Buyer`, `OrderItem`, `ShippingInfo`, `ReceiverAddress`, `Payment`), `MercadoLibreOrderFilter`, `CommonMercadoLibreOrder` container
- **jvmMain** &mdash; `IMercadoLibreOrderColl` abstract collection with `fetchOrders()` hook and concrete `syncToLocal()` bulk upsert logic
- **jvmMain** &mdash; Update `IMercadoLibreCollections` to expose `MercadoLibreOrderColl`
- **jsMain** &mdash; Column definition helpers for order list views (`colDefMercadoLibreOrder*.kt`)

## Out of Scope

- MercadoLibre API HTTP client (app responsibility &mdash; see `MercadoLibreIntegration(BRIDGE)`)
- Frontend views, routing, or RPC services (app responsibility)
- Order creation/mutation on ML API (read-only sync)
- Configuration UI for credentials or sync scheduling
- Shipping label generation or tracking integration

## Pattern Reference

Follows the same established pattern as `MercadoLibreProduct`:

| Layer | Product (existing) | Order (new) |
|-------|-------------------|-------------|
| Model | `MercadoLibreProduct` | `MercadoLibreOrder` + embedded types |
| Filter | `MercadoLibreProductFilter` | `MercadoLibreOrderFilter` |
| Common | `CommonMercadoLibreProduct` | `CommonMercadoLibreOrder` |
| Coll | `IMercadoLibreProductColl` | `IMercadoLibreOrderColl` |
| Collections | `IMercadoLibreCollections.MercadoLibreProductColl` | + `MercadoLibreOrderColl` |

## Key Decisions

- **Money** &mdash; Reuse `Money` value class (Long cents) from commonLib for all monetary fields
- **Dates** &mdash; `kotlinx.datetime.Instant` for all timestamps
- **ID strategy** &mdash; `StringId<MercadoLibreOrder>` (ML order IDs are numeric but stored as strings for consistency with product pattern)
- **Embedded types** &mdash; Buyer, OrderItem, ShippingInfo, Payment are nested `@Serializable data class` inside `MercadoLibreOrder` (same pattern as `MercadoLibreProduct.Picture`)
- **syncToRemote** &mdash; Returns "not supported" (ML orders are read-only from seller perspective)

---
[&larr; Index](../INDEX.md) &middot; **BRIEF** &middot; [SPEC](SPECIFICATION.md) &middot; [API CONTRACT](API_CONTRACT.md) &middot; [PLAN](IMPLEMENTATION_PLAN.md)
