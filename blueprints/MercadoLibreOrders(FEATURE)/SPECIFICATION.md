# MercadoLibreOrders &mdash; Technical Specification

## Feature Objective &amp; Scope

Add shared MercadoLibre order models and sync abstractions to `mercadoLibreLib`, enabling any consuming app to pull ML orders into local MongoDB storage. The feature follows the identical pattern established by `MercadoLibreProduct` (model + filter + CommonContainer + abstract Coll with `fetchOrders()` hook). Consuming apps implement the abstract `fetchOrders()` method with their ML API client; the library provides the concrete `syncToLocal()` bulk upsert logic.

## Functional Requirements

| ID | Requirement |
|----|-------------|
| REQ-LIB-ORD-01 | The library SHALL provide a `MercadoLibreOrder` data class with fields: `_id` (`StringId<MercadoLibreOrder>`), `marketplaceId` (`MarketplaceId`), `dateCreated` (`Instant`), `lastUpdated` (`Instant`), `status` (`String`), `totalAmount` (`Money`), `paidAmount` (`Money`), `currencyId` (`String`), `buyer` (`Buyer` embedded), `orderItems` (`List<OrderItem>`), `shipping` (`ShippingInfo` embedded), `payments` (`List<Payment>`), `packId` (`Long?`) |
| REQ-LIB-ORD-02 | The library SHALL provide a `Buyer` embedded type with fields: `id` (`Long`), `nickname` (`String`), `firstName` (`String`), `lastName` (`String`), `email` (`String`), `phone` (`String`) |
| REQ-LIB-ORD-03 | The library SHALL provide an `OrderItem` embedded type with fields: `title` (`String`), `quantity` (`Int`), `unitPrice` (`Money`), `sku` (`String`), `variationId` (`String?`) |
| REQ-LIB-ORD-04 | The library SHALL provide a `ShippingInfo` embedded type with fields: `id` (`Long?`), `status` (`String`), `trackingNumber` (`String?`), `receiverAddress` (`ReceiverAddress` embedded) |
| REQ-LIB-ORD-04a | The library SHALL provide a `ReceiverAddress` embedded type with fields: `zipCode` (`String`), `city` (`String`), `state` (`String`) |
| REQ-LIB-ORD-05 | The library SHALL provide a `Payment` embedded type with fields: `id` (`Long`), `status` (`String`), `transactionAmount` (`Money`), `paymentMethodId` (`String`) |
| REQ-LIB-ORD-06 | The library SHALL provide a `MercadoLibreOrderFilter` data class implementing `IApiFilter<MarketplaceId>` with fields: `masterItemId` (`MarketplaceId?`), `status` (`String?`), `dateFrom` (`Instant?`), `dateTo` (`Instant?`) |
| REQ-LIB-ORD-07 | The library SHALL provide a `CommonMercadoLibreOrder` data object implementing `ICommonContainer` with `itemKClass`, `filterKClass`, `labelItem`, and `labelList` |
| REQ-LIB-ORD-08 | The library SHALL provide an `IMercadoLibreOrderColl` abstract class extending `Coll` and `IMarketplaceSync`, with abstract `fetchOrders(marketplace): List<MercadoLibreOrder>` and concrete `syncToLocal(marketplace): SimpleState` using bulk upsert |
| REQ-LIB-ORD-09 | The library SHALL update `IMercadoLibreCollections` to include an abstract `MercadoLibreOrderColl: IMercadoLibreOrderColl<*>` property |

## Business Rules &amp; Constraints

| ID | Rule |
|----|------|
| BR-ORD-01 | All monetary fields (`totalAmount`, `paidAmount`, `unitPrice`, `transactionAmount`) MUST use the `Money` value class (`Long` cents) &mdash; no floating point |
| BR-ORD-02 | All date/time fields (`dateCreated`, `lastUpdated`) MUST use `kotlinx.datetime.Instant` |
| BR-ORD-03 | ML numeric IDs stored as `Long` for buyer, payment, and shipping; order `_id` stored as `StringId` for consistency with the product pattern |
| BR-ORD-04 | `syncToLocal()` performs bulk upsert by `_id` (same as `IMercadoLibreProductColl`) |
| BR-ORD-05 | `syncToRemote()` returns `SimpleState(isOk = false, "syncToRemote not supported for MercadoLibre orders")` &mdash; orders are read-only |
| BR-ORD-06 | `fetchOrders()` failures are caught and returned as `simpleErrorState` without crashing the sync loop |
| BR-ORD-07 | Empty order list from `fetchOrders()` is a valid success case (no orders to sync) |
| BR-ORD-08 | `packId` is nullable &mdash; ML packs group multiple orders into a single shipment; not all orders belong to a pack |

## Data Models

### MercadoLibreOrder (root entity)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `_id` | `StringId<MercadoLibreOrder>` | &mdash; | ML order ID as string |
| `marketplaceId` | `MarketplaceId` | &mdash; | Reference to Marketplace |
| `dateCreated` | `Instant` | &mdash; | Order creation timestamp |
| `lastUpdated` | `Instant` | &mdash; | Last modification timestamp |
| `status` | `String` | `""` | ML order status (paid, shipped, delivered, cancelled, etc.) |
| `totalAmount` | `Money` | `Money.ZERO` | Total order amount |
| `paidAmount` | `Money` | `Money.ZERO` | Amount actually paid |
| `currencyId` | `String` | `"MXN"` | Currency code |
| `buyer` | `Buyer` | &mdash; | Embedded buyer info |
| `orderItems` | `List<OrderItem>` | `emptyList()` | Line items |
| `shipping` | `ShippingInfo` | &mdash; | Embedded shipping info |
| `payments` | `List<Payment>` | `emptyList()` | Payment records |
| `packId` | `Long?` | `null` | ML pack grouping ID |

### Buyer (embedded)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `id` | `Long` | &mdash; | ML buyer user ID |
| `nickname` | `String` | `""` | ML username |
| `firstName` | `String` | `""` | Buyer first name |
| `lastName` | `String` | `""` | Buyer last name |
| `email` | `String` | `""` | Buyer email |
| `phone` | `String` | `""` | Buyer phone |

### OrderItem (embedded)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `title` | `String` | `""` | Product title |
| `quantity` | `Int` | `1` | Units purchased |
| `unitPrice` | `Money` | `Money.ZERO` | Price per unit |
| `sku` | `String` | `""` | Seller SKU |
| `variationId` | `String?` | `null` | ML variation ID (if applicable) |

### ShippingInfo (embedded)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `id` | `Long?` | `null` | ML shipping ID |
| `status` | `String` | `""` | Shipping status |
| `trackingNumber` | `String?` | `null` | Carrier tracking number |
| `receiverAddress` | `ReceiverAddress` | &mdash; | Delivery address |

### ReceiverAddress (embedded)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `zipCode` | `String` | `""` | Postal code |
| `city` | `String` | `""` | City name |
| `state` | `String` | `""` | State/province |

### Payment (embedded)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `id` | `Long` | &mdash; | ML payment ID |
| `status` | `String` | `""` | Payment status (approved, rejected, etc.) |
| `transactionAmount` | `Money` | `Money.ZERO` | Amount charged |
| `paymentMethodId` | `String` | `""` | Payment method identifier |

## Data Conversion Rules

| Source (ML API) | Target (Model) | Rule |
|-----------------|----------------|------|
| `order.total_amount` (Double) | `totalAmount: Money` | Multiply by 100, convert to Long |
| `order.paid_amount` (Double) | `paidAmount: Money` | Multiply by 100, convert to Long |
| `item.unit_price` (Double) | `unitPrice: Money` | Multiply by 100, convert to Long |
| `payment.transaction_amount` (Double) | `transactionAmount: Money` | Multiply by 100, convert to Long |
| `order.date_created` (ISO 8601 string) | `dateCreated: Instant` | Parse via `Instant.parse()` |
| `order.last_updated` (ISO 8601 string) | `lastUpdated: Instant` | Parse via `Instant.parse()` |
| `order.id` (Long) | `_id: StringId` | Convert to string via `.toString()` |

## Relationships

```
Marketplace (commonLib)
    |
    +--< MercadoLibreProduct (existing)
    |
    +--< MercadoLibreOrder (new)
              |
              +--- Buyer (embedded)
              +--< OrderItem (embedded list)
              +--- ShippingInfo (embedded)
              |        +--- ReceiverAddress (embedded)
              +--< Payment (embedded list)
```

---
[&larr; Index](../INDEX.md) &middot; [BRIEF](BRIEF.md) &middot; **SPEC** &middot; [API CONTRACT](API_CONTRACT.md) &middot; [PLAN](IMPLEMENTATION_PLAN.md)
