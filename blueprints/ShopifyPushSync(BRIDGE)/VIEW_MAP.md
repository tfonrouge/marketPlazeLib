# ShopifyPushSync — View Map

## New Views

### ViewListShopifyCollection — NEW
- **Type**: Page (Tabulator list)
- **Route**: app-defined
- **Actor**: App admin
- **Purpose**: Browse Shopify collections synced from Shopify, see product count per collection
- **Key Elements**: Tabulator with columns: title, handle, productsCount, sortOrder
- **Data Sources**: ShopifyCollectionColl via RPC list service
- **Empty State**: "No collections synced. Run sync to pull collections from Shopify."

### SyncDashboard — NEW (consumer app responsibility)
- **Type**: Page
- **Actor**: App admin
- **Purpose**: View last sync results, trigger manual sync, see reconciliation report
- **Key Elements**: Sync status cards (last pull, last push, last reconcile), action buttons, SyncReport table
- **Data Sources**: SyncReport from reconcile(), SimpleState from sync operations
- **Note**: This is a consumer app view — the library provides the data (SyncReport), the app builds the UI

## Modified Views

### ViewItemProduct — MODIFIED
- **Delta**: Add form fields for new Product attributes: erpSourceId, priceLists (sub-table), inventoryQuantity, descriptionHtml (rich text editor), publishEnabled (checkbox), status (select), tags (multi-input), weight + weightUnit, barcode, seoTitle, seoDescription
- **Note**: The library's `productFormFields()` builder will be extended with these fields. Consuming apps that already use it will get them automatically.

## Navigation Changes

- Add ViewListShopifyCollection to app navigation (consumer responsibility)
- Add SyncDashboard to app navigation (consumer responsibility)

## Shared Components

- `colDefCollectionTitle()`, `colDefCollectionHandle()`, `colDefCollectionProductsCount()` — new column definitions for ShopifyCollection
- Extended `productFormFields()` — enriched form builder with all new fields

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · **VIEWS** · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md)
