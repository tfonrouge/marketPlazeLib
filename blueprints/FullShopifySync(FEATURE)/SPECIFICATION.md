# SPECIFICATION — FullShopifySync (FEATURE)

## 1. Library Objective & Scope

Enhance marketPlazeLib from v1.3.0 to v2.0.0 with full Shopify sync capabilities: smart collection CRUD, image processing pipeline, enhanced field-level diff engine, and bulk sync operations. Stabilizes all dependencies on released (non-SNAPSHOT) versions.

## 2. Functional Requirements

### Shopify Collection CRUD

| ID | Requirement |
|----|-------------|
| **FR-MPL-001** | The library SHALL provide `createCollection(marketplace, collection)` in `IShopifyPushService` that creates a Shopify smart collection with title, rules, sort order, description, and image via `collectionCreate` GraphQL mutation |
| **FR-MPL-002** | The library SHALL provide `updateCollection(marketplace, collection)` that updates an existing collection via `collectionUpdate` GraphQL mutation (ID read from `collection._id`) |
| **FR-MPL-003** | The library SHALL provide `deleteCollection(marketplace, collectionId)` that removes a collection via `collectionDelete` GraphQL mutation |
| **FR-MPL-004** | `ShopifyCollection` SHALL include `rules: List<CollectionRule>`, `isSmartCollection: Boolean`, `image: String?` |
| **FR-MPL-005** | `CollectionRule` SHALL be a data class: `column: String, relation: String, condition: String` matching Shopify's `CollectionRuleInput` schema field naming |

### Image Processing Pipeline

| ID | Requirement |
|----|-------------|
| **FR-MPL-006** | The library SHALL provide `ImageProcessor` (JVM-only) with fluent pipeline: `resize(maxWidth, maxHeight)`, `watermark(text, position)`, `toJpeg(quality)`, `toPng()`, `process(inputBytes): ByteArray` |
| **FR-MPL-007** | The library SHALL support image size variants: BASE (470px), SMALL (320px), THUMBNAIL (100px) via an `ImageSize` enum |
| **FR-MPL-008** | The library SHALL provide SHA-256 deduplication: `hashImage(bytes): String` to prevent duplicate uploads |
| **FR-MPL-009** | `IShopifyPushService` SHALL provide `uploadImageFromBytes(marketplace, shopifyId, imageBytes, filename, altText?)` using Shopify's `stagedUploadsCreate` + `productCreateMedia` flow |

### Field-Level Diff Engine

| ID | Requirement |
|----|-------------|
| **FR-MPL-010** | `ShopifyBidirectionalSyncEngine.detectChanges()` SHALL compare: title, descriptionHtml, price, barcode, vendor, weight, tags, published status, image count, productType |
| **FR-MPL-011** | `SyncDiff.changedFields` SHALL use `List<FieldChange>` where `FieldChange(fieldName: String, oldValue: String?, newValue: String?)` — BREAKING CHANGE replacing `List<String>` |

### Bulk Sync Operations

| ID | Requirement |
|----|-------------|
| **FR-MPL-012** | `ShopifyBidirectionalSyncEngine` SHALL provide `bulkSyncToRemote(marketplace, batchSize=50, delayMs=500): BulkSyncResult` that batches product creates and updates with rate limiting between Shopify API calls |
| **FR-MPL-013** | `bulkSyncToRemote` SHALL reconcile first, then execute creates and updates in configurable batches, accumulating errors into `BulkSyncResult` |
| **FR-MPL-014** | `BulkSyncResult` SHALL report `created: Int, updated: Int, failed: Int, errors: List<SyncError>` |

### Product Query & Smoke Testing

| ID | Requirement |
|----|-------------|
| **FR-MPL-018** | `IShopifyPushService` SHALL provide `getProduct(marketplace, shopifyId): ItemState<Product>` that fetches a single product's current state from Shopify via `GetProductDetailQuery` |
| **FR-MPL-019** | The library SHALL provide `ShopifySmokeTest` (JVM-only) that performs a self-reverting round-trip test: fetch product → mutate one field → push → verify change → revert → verify revert. The test SHALL use `try/finally` to guarantee revert even on failure |
| **FR-MPL-020** | `ShopifySmokeTest` SHALL return a `SmokeTestResult` with: `productId`, `fieldTested`, `originalValue`, `testValue`, `updateVerified: Boolean`, `revertVerified: Boolean`, `error: String?` |

### Dependency Stabilization

| ID | Requirement |
|----|-------------|
| **FR-MPL-015** | All SNAPSHOT dependencies SHALL be replaced: Kotlin 2.3.20, fsLib 3.2.0, KVision 9.5.0, kilua-rpc 0.0.43, KSP 2.3.6 |
| **FR-MPL-016** | Apollo GraphQL codegen SHALL work with updated Kotlin/KSP |
| **FR-MPL-017** | All existing 43 tests SHALL pass after dependency updates |

## 3. Business Rules & Constraints

| ID | Rule |
|----|------|
| **BR-MPL-001** | Smart collection rule fields: tag, type, title, vendor, variant_price, variant_compare_at_price, variant_weight, variant_inventory, variant_title. Relations: equals, not_equals, starts_with, ends_with, contains, not_contains, greater_than, less_than |
| **BR-MPL-002** | Image resize preserves aspect ratio; maxWidth/maxHeight are upper bounds |
| **BR-MPL-003** | Watermark defaults to BOTTOM_RIGHT, 50% opacity |
| **BR-MPL-004** | Bulk operations use 500ms delay between Shopify API calls |
| **BR-MPL-005** | `FieldChange.oldValue` and `newValue` are `String?` (serializable) |
| **BR-MPL-006** | Image maximum size per Shopify: 20MB. Pipeline validates before upload |
| **BR-MPL-007** | `ShopifySmokeTest` uses `title` as the test field — appends ` [SMOKE TEST]` suffix, then reverts to original. Title is chosen because it's visible, has no side effects, and is a simple string comparison |
| **BR-MPL-008** | `ShopifySmokeTest` MUST revert the product even if the verification step fails. Uses `try/finally` to guarantee cleanup |
| **BR-MPL-009** | `ShopifySmokeTest` requires a valid `Marketplace` with live credentials and an existing Shopify product GID. It is NOT safe for automated CI — designed for manual integration verification |

## 4. Consumer Roles

| Consumer | Usage |
|----------|-------|
| **Any ERP-connected app** | Implements `IErpProductReader`, wires `ShopifyBidirectionalSyncEngine` + `ShopifyPushEngine` |
| **Standalone Shopify tools** | Uses `IShopifyPushService` directly for collection/image/product management |
| **samples module** | Reference implementation updated with new features |

## 5. Data Models & Relationships

### New Models

| Model | Module | Fields |
|-------|--------|--------|
| `CollectionRule` | shopifyLib | `column: String, relation: String, condition: String` |
| `FieldChange` | shopifyLib | `fieldName: String, oldValue: String?, newValue: String?` |
| `BulkSyncResult` | shopifyLib | `created: Int, updated: Int, failed: Int, errors: List<SyncError>` |
| `ImageSize` | commonLib | Enum: `BASE(470), SMALL(320), THUMBNAIL(100)` |

### Modified Models (BREAKING)

| Model | Change |
|-------|--------|
| `ShopifyCollection` | Add `rules: List<CollectionRule>`, `isSmartCollection: Boolean`, `image: String?` |
| `SyncDiff` | `changedFields: List<String>` → `changedFields: List<FieldChange>` |

## 6. Integration Points

| Integration | Direction | Data |
|-------------|-----------|------|
| Shopify Admin GraphQL API (2025-10) | Outbound | Products, collections, media, inventory |
| fsLib MongoDB | Internal | Collection persistence |
| java.awt / Thumbnailator | Internal (JVM) | Image processing |

## 7. Performance Requirements

| Metric | Requirement |
|--------|-------------|
| Bulk create 100 products | < 60 seconds (500ms rate limit) |
| Image resize 800x800 | < 200ms per image |
| Diff detection 1000 products | < 5 seconds |

## 8. Platform/Target Constraints

| API | JVM | JS |
|-----|-----|-----|
| ImageProcessor | ✅ | ❌ (JVM-only) |
| ShopifyPushEngine, sync engines | ✅ | ❌ (server-only) |
| Models, filters, Money | ✅ | ✅ |
| KVision column defs, components | ❌ | ✅ |

---

[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · **SPEC** · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · PLAN · TESTS · AUDIT
