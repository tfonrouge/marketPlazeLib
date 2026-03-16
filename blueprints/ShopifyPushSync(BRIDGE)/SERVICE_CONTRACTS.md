# ShopifyPushSync — Service Contracts

## Contract Summary

| Direction | Caller → Callee | Method | Purpose |
|-----------|----------------|--------|---------|
| Consumer → Library | App → IErpProductReader | readProducts() | App provides ERP data |
| Consumer → Library | App → IErpProductReader | readProductById() | App provides single product |
| Consumer → Library | App → IErpProductReader | readInventoryLevels() | App provides inventory |
| Consumer → Library | App → IErpProductReader | readPriceLists() | App provides prices |
| Library → Library | Engine → IMarketplaceSync | syncToRemote() | Push trigger |
| Library → Shopify | IShopifyPushService → Shopify API | createProduct() | Create on Shopify |
| Library → Shopify | IShopifyPushService → Shopify API | updateProduct() | Update on Shopify |
| Library → Shopify | IShopifyPushService → Shopify API | deleteProduct() | Delete on Shopify |
| Library → Shopify | IShopifyPushService → Shopify API | updateInventory() | Set inventory |
| Library → Shopify | IShopifyPushService → Shopify API | setPublishState() | Publish/unpublish |
| Library → Shopify | IShopifyPushService → Shopify API | addToCollection() | Collection membership |
| Library → Shopify | IShopifyPushService → Shopify API | removeFromCollection() | Collection membership |
| Library → Shopify | IShopifyPushService → Shopify API | uploadImage() | Staged image upload |
| Library → Shopify | IShopifyPushService → Shopify API | deleteImage() | Remove image |
| Library → Shopify | IShopifyPushService → Shopify API | reorderImages() | Reorder images |

## IErpProductReader — Consumer → Library

**Direction**: Consumer app implements this interface; the sync engine calls it.
**Owner**: Consumer app team

```kotlin
interface IErpProductReader {
    suspend fun readProducts(): List<Product>
    suspend fun readProductById(erpId: String): Product?
    suspend fun readInventoryLevels(): Map<String, Int>   // sku → quantity
    suspend fun readPriceLists(): Map<String, List<PriceListEntry>>  // sku → prices
}
```

### readProducts()
- **Trigger**: Called by ShopifyBidirectionalSyncEngine.syncErpToShopify()
- **Input**: None
- **Output**: `List<Product>` — all products the consumer wants to manage
- **Side Effects**: None (read-only)
- **Error Handling**: Throws exception on failure → engine wraps in SimpleState error

### readInventoryLevels()
- **Trigger**: Called during push sync to get current stock levels
- **Input**: None
- **Output**: `Map<String, Int>` — SKU to quantity mapping
- **Side Effects**: None

## IMarketplaceSync — Extended Interface

**Direction**: Library defines; sync engines implement.
**Breaking change**: Adds `syncToRemote()` to existing interface.

```kotlin
interface IMarketplaceSync<T : BaseDoc<*>> {
    suspend fun syncToLocal(marketplace: Marketplace): SimpleState   // existing
    suspend fun syncToRemote(marketplace: Marketplace): SimpleState  // NEW
}
```

## IShopifyPushService — Library → Shopify API

**Direction**: shopifyLib implements; uses Apollo GraphQL mutations.
**Owner**: shopifyLib module

### createProduct(marketplace, product)
- **Trigger**: Product exists in ERP with publishEnabled=true, no MarketplaceListing exists
- **Input**: `Marketplace` (for credentials), `Product` (full product data)
- **Output**: `SimpleState` with shopify product GID in msgOk
- **Side Effects**: Creates product on Shopify, emits no events
- **GraphQL**: `mutation productCreate`

### updateProduct(marketplace, product, shopifyId)
- **Trigger**: MarketplaceListing exists, ERP data has changed
- **Input**: `Marketplace`, `Product`, `shopifyId: String`
- **Output**: `SimpleState`
- **Side Effects**: Updates product on Shopify
- **GraphQL**: `mutation productUpdate`

### deleteProduct(marketplace, shopifyId)
- **Trigger**: Manual deletion or cleanup of orphaned products
- **Input**: `Marketplace`, `shopifyId: String`
- **Output**: `SimpleState`
- **GraphQL**: `mutation productDelete`

### updateInventory(marketplace, shopifyId, locationId, quantity)
- **Trigger**: Inventory levels changed in ERP
- **Input**: `Marketplace`, `shopifyId`, `locationId`, `quantity: Int`
- **Output**: `SimpleState`
- **GraphQL**: `mutation inventorySetQuantities`

### setPublishState(marketplace, shopifyId, published)
- **Trigger**: Product publishEnabled changed in ERP
- **Input**: `Marketplace`, `shopifyId`, `published: Boolean`
- **Output**: `SimpleState`
- **GraphQL**: `mutation publishablePublish` / `publishableUnpublish`

### addToCollection(marketplace, collectionId, productId) / removeFromCollection(...)
- **Trigger**: Collection membership changed
- **GraphQL**: `mutation collectionAddProducts` / `collectionRemoveProducts`

### uploadImage(marketplace, shopifyId, imageUrl, altText) / deleteImage / reorderImages
- **Trigger**: Image changes detected during sync
- **GraphQL**: `stagedUploadsCreate` → HTTP POST → `productCreateMedia` / `productDeleteMedia` / `productReorderMedia`

## Events

No domain events emitted. All operations are synchronous request/response via GraphQL mutations.

## Shared Entities

| Entity | Shared Between | Notes |
|--------|---------------|-------|
| `Product` | commonLib ↔ consumer app | Enriched with ERP fields |
| `MarketplaceListing` | commonLib ↔ shopifyLib | `externalId` stores Shopify GID |
| `Marketplace` | commonLib ↔ shopifyLib | `connectionConfig` provides Shopify credentials |
| `ShopifyCollection` | shopifyLib ↔ consumer app | New entity |
