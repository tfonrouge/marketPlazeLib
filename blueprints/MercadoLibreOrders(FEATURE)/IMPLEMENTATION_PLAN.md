# MercadoLibreOrders &mdash; Implementation Plan

**Status:** Planning

## Phase 1: Model + Collection + View Helpers (single phase)

**Deliverable:** Complete MercadoLibreOrder feature in mercadoLibreLib
**Estimated Effort:** 1 dev-day
**Owner:** Teo / Claude
**Dependencies:** None (mercadoLibreLib already has MercadoLibreProduct as reference pattern)

### Tasks

| # | Task | Source Set | File | REQ |
|---|------|-----------|------|-----|
| 1 | Create `MercadoLibreOrder` data class with embedded types (`Buyer`, `OrderItem`, `ShippingInfo`, `ReceiverAddress`, `Payment`) | commonMain | `model/MercadoLibreOrder.kt` | REQ-LIB-ORD-01, 02, 03, 04, 04a, 05 |
| 2 | Create `MercadoLibreOrderFilter` | commonMain | `model/MercadoLibreOrderFilter.kt` | REQ-LIB-ORD-06 |
| 3 | Create `CommonMercadoLibreOrder` data object | commonMain | `common/CommonMercadoLibreOrder.kt` | REQ-LIB-ORD-07 |
| 4 | Create `IMercadoLibreOrderColl` abstract class with `fetchOrders()` hook and concrete `syncToLocal()` / `syncToRemote()` | jvmMain | `colls/IMercadoLibreOrderColl.kt` | REQ-LIB-ORD-08 |
| 5 | Update `IMercadoLibreCollections` to add abstract `MercadoLibreOrderColl` property | jvmMain | `colls/IMercadoLibreCollections.kt` | REQ-LIB-ORD-09 |
| 6 | Add jsMain column definition helpers for order list views | jsMain | `view/colDef/colDefMercadoLibreOrder.kt` | &mdash; |
| 7 | `publishToMavenLocal` and verify clean compilation | &mdash; | &mdash; | &mdash; |

### Task Details

#### Task 1 &mdash; MercadoLibreOrder model

**Path:** `mercadoLibreLib/src/commonMain/kotlin/com/fonrouge/mercadoLibreLib/model/MercadoLibreOrder.kt`

- `@Serializable data class MercadoLibreOrder` extending `BaseDoc<StringId<MercadoLibreOrder>>`
- 5 embedded `@Serializable data class` types nested inside the companion or as top-level in the same file: `Buyer`, `OrderItem`, `ShippingInfo`, `ReceiverAddress`, `Payment`
- All monetary fields typed as `Money` with default `Money.ZERO`
- Date fields typed as `kotlinx.datetime.Instant`
- Follow same nesting pattern as `MercadoLibreProduct.Picture`

#### Task 2 &mdash; MercadoLibreOrderFilter

**Path:** `mercadoLibreLib/src/commonMain/kotlin/com/fonrouge/mercadoLibreLib/model/MercadoLibreOrderFilter.kt`

- `@Serializable data class MercadoLibreOrderFilter` extending `IApiFilter<MarketplaceId>()`
- `override var masterItemId: MarketplaceId?` (required by `IApiFilter`)
- Additional filter fields: `status`, `dateFrom`, `dateTo`

#### Task 3 &mdash; CommonMercadoLibreOrder

**Path:** `mercadoLibreLib/src/commonMain/kotlin/com/fonrouge/mercadoLibreLib/common/CommonMercadoLibreOrder.kt`

- `data object CommonMercadoLibreOrder : ICommonContainer<...>`
- Labels: `"MercadoLibre Order"` / `"MercadoLibre Orders"`
- Mirror `CommonMercadoLibreProduct` pattern exactly

#### Task 4 &mdash; IMercadoLibreOrderColl

**Path:** `mercadoLibreLib/src/jvmMain/kotlin/com/fonrouge/mercadoLibreLib/colls/IMercadoLibreOrderColl.kt`

- `abstract class IMercadoLibreOrderColl<UID : Any>` extending `Coll<...>` and `IMarketplaceSync<MercadoLibreOrder>`
- `abstract suspend fun fetchOrders(marketplace: Marketplace): List<MercadoLibreOrder>`
- `override suspend fun syncToLocal(marketplace: Marketplace): SimpleState` &mdash; concrete, bulk upsert (copy from `IMercadoLibreProductColl.syncToLocal`)
- `override suspend fun syncToRemote(marketplace: Marketplace): SimpleState` &mdash; returns "not supported"

#### Task 5 &mdash; Update IMercadoLibreCollections

**Path:** `mercadoLibreLib/src/jvmMain/kotlin/com/fonrouge/mercadoLibreLib/colls/IMercadoLibreCollections.kt`

- Add `abstract val MercadoLibreOrderColl: IMercadoLibreOrderColl<*>` alongside existing `MercadoLibreProductColl`

#### Task 6 &mdash; Column definition helpers (jsMain)

**Path:** `mercadoLibreLib/src/jsMain/kotlin/com/fonrouge/mercadoLibreLib/view/colDef/colDefMercadoLibreOrder.kt`

- Column definition functions for common order fields: status, totalAmount, paidAmount, buyer nickname, dateCreated, shipping status
- Follow pattern from `commonLib/src/jsMain/.../colDef/colDefProduct.kt`

#### Task 7 &mdash; Publish

```bash
./gradlew publishToMavenLocal
```

Verify:
- Clean compilation across all source sets (commonMain, jvmMain, jsMain)
- No breaking changes to existing code
- Consuming apps can resolve the new version

### File Summary

| Source Set | New Files | Modified Files |
|-----------|-----------|----------------|
| commonMain | `model/MercadoLibreOrder.kt`, `model/MercadoLibreOrderFilter.kt`, `common/CommonMercadoLibreOrder.kt` | &mdash; |
| jvmMain | `colls/IMercadoLibreOrderColl.kt` | `colls/IMercadoLibreCollections.kt` |
| jsMain | `view/colDef/colDefMercadoLibreOrder.kt` | &mdash; |

## Risk Register

| Risk | Impact | Mitigation | Status |
|------|--------|------------|--------|
| Consuming apps must update their `IMercadoLibreCollections` subclass to provide `MercadoLibreOrderColl` | Medium &mdash; compile error in consumers | Document in CHANGELOG; single abstract property to add | Open |
| ML Order API response fields may vary by country/category | Low &mdash; model uses sensible defaults | All fields except `_id` and `marketplaceId` have defaults | Accepted |
| Money conversion precision loss from ML API doubles | Low &mdash; standard cent conversion | Document `(amount * 100).toLong()` pattern in data conversion rules | Accepted |

## Traceability

| REQ | Task | File |
|-----|------|------|
| REQ-LIB-ORD-01 | 1 | `model/MercadoLibreOrder.kt` |
| REQ-LIB-ORD-02 | 1 | `model/MercadoLibreOrder.kt` (Buyer) |
| REQ-LIB-ORD-03 | 1 | `model/MercadoLibreOrder.kt` (OrderItem) |
| REQ-LIB-ORD-04 | 1 | `model/MercadoLibreOrder.kt` (ShippingInfo) |
| REQ-LIB-ORD-04a | 1 | `model/MercadoLibreOrder.kt` (ReceiverAddress) |
| REQ-LIB-ORD-05 | 1 | `model/MercadoLibreOrder.kt` (Payment) |
| REQ-LIB-ORD-06 | 2 | `model/MercadoLibreOrderFilter.kt` |
| REQ-LIB-ORD-07 | 3 | `common/CommonMercadoLibreOrder.kt` |
| REQ-LIB-ORD-08 | 4 | `colls/IMercadoLibreOrderColl.kt` |
| REQ-LIB-ORD-09 | 5 | `colls/IMercadoLibreCollections.kt` |

---
[&larr; Index](../INDEX.md) &middot; [BRIEF](BRIEF.md) &middot; [SPEC](SPECIFICATION.md) &middot; [API CONTRACT](API_CONTRACT.md) &middot; **PLAN**
