# MercadoLibreOrders &mdash; API Contract

## Overview

This document defines the internal library interfaces exposed by the MercadoLibreOrders feature. These are **not** HTTP endpoints &mdash; they are Kotlin abstract/concrete methods that consuming apps interact with programmatically.

## Collection Interface: IMercadoLibreOrderColl

### Class Signature

```kotlin
abstract class IMercadoLibreOrderColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreOrder, StringId<MercadoLibreOrder>, MercadoLibreOrderFilter, UID>(
    commonContainer = CommonMercadoLibreOrder,
    debug = debug,
), IMarketplaceSync<MercadoLibreOrder>
```

### Methods

#### `fetchOrders(marketplace)` &mdash; Abstract

| Property | Value |
|----------|-------|
| Visibility | `abstract suspend` |
| Signature | `fetchOrders(marketplace: Marketplace): List<MercadoLibreOrder>` |
| Returns | List of `MercadoLibreOrder` fetched from ML API |
| Responsibility | **App implements** &mdash; calls ML Orders API, converts JSON to model |
| Error handling | May throw exceptions; caller (`syncToLocal`) catches them |

**Implementation contract for consuming apps:**
1. Use `marketplace.connectionConfig` to obtain ML API credentials (access token, seller ID)
2. Call ML `/orders/search` endpoint with appropriate filters
3. Map ML API JSON response fields to `MercadoLibreOrder` model using conversion rules from SPECIFICATION
4. Return the complete list; do not persist (that is `syncToLocal`'s job)

#### `syncToLocal(marketplace)` &mdash; Concrete

| Property | Value |
|----------|-------|
| Visibility | `override suspend` |
| Signature | `syncToLocal(marketplace: Marketplace): SimpleState` |
| Returns | `SimpleState` with success/error message and timing info |
| Responsibility | **Library provides** &mdash; calls `fetchOrders()`, bulk upserts to MongoDB |

**Behavior:**
1. Record `initTime`
2. Call `fetchOrders(marketplace)` inside try/catch
3. On exception: return `simpleErrorState("MercadoLibre order fetch error: ${e.message}")`
4. On empty list: return `SimpleState(isOk = true, "No orders found, $useTime ms used")`
5. Build `List<UpdateOneModel>` with upsert by `_id`
6. Execute `coroutine.bulkWrite(updateOneModels)`
7. Return success with modified count and timing, or error if not acknowledged

#### `syncToRemote(marketplace)` &mdash; Concrete (stub)

| Property | Value |
|----------|-------|
| Visibility | `override suspend` |
| Signature | `syncToRemote(marketplace: Marketplace): SimpleState` |
| Returns | `SimpleState(isOk = false, "syncToRemote not supported for MercadoLibre orders")` |
| Responsibility | **Library provides** &mdash; ML orders are read-only from seller API |

## Collections Factory: IMercadoLibreCollections

### Updated Signature

```kotlin
abstract class IMercadoLibreCollections<UID : Any> : MarketPlazeCollections() {
    abstract val MercadoLibreProductColl: IMercadoLibreProductColl<*>  // existing
    abstract val MercadoLibreOrderColl: IMercadoLibreOrderColl<*>     // new
}
```

## CommonContainer: CommonMercadoLibreOrder

```kotlin
data object CommonMercadoLibreOrder : ICommonContainer<
    MercadoLibreOrder,
    StringId<MercadoLibreOrder>,
    MercadoLibreOrderFilter
>(
    itemKClass = MercadoLibreOrder::class,
    filterKClass = MercadoLibreOrderFilter::class,
    labelItem = "MercadoLibre Order",
    labelList = "MercadoLibre Orders",
)
```

## Filter: MercadoLibreOrderFilter

```kotlin
@Serializable
data class MercadoLibreOrderFilter(
    override var masterItemId: MarketplaceId? = null,
    val status: String? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
) : IApiFilter<MarketplaceId>()
```

## Error Handling

All sync operations return `SimpleState`:

| Scenario | `isOk` | Message pattern |
|----------|--------|-----------------|
| Successful sync | `true` | `"Updated N documents, X ms used"` |
| No orders found | `true` | `"No orders found, X ms used"` |
| Fetch exception | `false` | `"MercadoLibre order fetch error: <message>"` |
| Bulk write not acknowledged | `false` | `"Bulk write operation was not acknowledged"` |
| syncToRemote called | `false` | `"syncToRemote not supported for MercadoLibre orders"` |

## Consumer Integration Example

```kotlin
// In consuming app (e.g., mppErsaPack jvmMain)
class MercadoLibreOrderColl : IMercadoLibreOrderColl<ObjectId>() {
    override suspend fun fetchOrders(marketplace: Marketplace): List<MercadoLibreOrder> {
        val accessToken = marketplace.connectionConfig["accessToken"]
            ?: error("Missing ML access token")
        val sellerId = marketplace.connectionConfig["sellerId"]
            ?: error("Missing ML seller ID")

        // Call ML API, map response to MercadoLibreOrder list
        return mlApiClient.getOrders(accessToken, sellerId).map { it.toMercadoLibreOrder() }
    }
}
```

---
[&larr; Index](../INDEX.md) &middot; [BRIEF](BRIEF.md) &middot; [SPEC](SPECIFICATION.md) &middot; **API CONTRACT** &middot; [PLAN](IMPLEMENTATION_PLAN.md)
