# API_SURFACE — FullShopifySync (FEATURE)

> **Library:** `marketPlazeLib` v2.0.0
> **Published Coordinates:** `com.fonrouge.marketPlazeLib:{module}:2.0.0` (Maven Local)

---

## 1. Published Artifacts

| Artifact | Group | Version | Targets |
|----------|-------|---------|---------|
| `commonLib` | `com.fonrouge.marketPlazeLib` | 2.0.0 | JVM + JS |
| `shopifyLib` | `com.fonrouge.marketPlazeLib` | 2.0.0 | JVM + JS |
| `mercadoLibreLib` | `com.fonrouge.marketPlazeLib` | 2.0.0 | JVM + JS |
| `amazonLib` | `com.fonrouge.marketPlazeLib` | 2.0.0 | JVM + JS |

---

## 2. New / Modified Entities

### `CollectionRule` — NEW (shopifyLib/commonMain)

```kotlin
@Serializable
data class CollectionRule(
    val column: String,     // "tag", "type", "vendor", etc.
    val relation: String,   // "equals", "contains", "starts_with", etc.
    val condition: String,
)
```

### `ShopifyCollection` — MODIFIED (shopifyLib/commonMain)

New fields:
```kotlin
val rules: List<CollectionRule> = emptyList()
val isSmartCollection: Boolean = false
val image: String? = null
```

### `FieldChange` — NEW (shopifyLib/commonMain)

```kotlin
@Serializable
data class FieldChange(
    val fieldName: String,
    val oldValue: String? = null,
    val newValue: String? = null,
)
```

### `SyncDiff` — MODIFIED (BREAKING)

Change: `changedFields: List<String>` → `changedFields: List<FieldChange>`

### `BulkSyncResult` — NEW (shopifyLib/commonMain)

```kotlin
@Serializable
data class BulkSyncResult(
    val created: Int,
    val updated: Int,
    val failed: Int,
    val errors: List<SyncError>,
)
```

### `ImageSize` — NEW (commonLib/commonMain)

```kotlin
enum class ImageSize(val maxDimension: Int) {
    BASE(470), SMALL(320), THUMBNAIL(100)
}
```

---

## 3. Service Interfaces

### `IShopifyPushService` — EXTENDED (shopifyLib/commonMain)

Existing 10 methods unchanged. New methods:

```kotlin
// Collection CRUD (FR-MPL-001..003)
suspend fun createCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState
suspend fun updateCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState  // ID read from collection._id
suspend fun deleteCollection(marketplace: Marketplace, collectionId: String): SimpleState

// Binary image upload (FR-MPL-009)
suspend fun uploadImageFromBytes(
    marketplace: Marketplace,
    shopifyProductId: String,
    imageBytes: ByteArray,
    filename: String,
    altText: String? = null,
): SimpleState

// Bulk operations live on ShopifyBidirectionalSyncEngine, not IShopifyPushService:
// suspend fun bulkSyncToRemote(marketplace: Marketplace, batchSize: Int = 50, delayMs: Long = 500): BulkSyncResult
```

### `ImageProcessor` — NEW (commonLib/jvmMain)

```kotlin
class ImageProcessor {
    fun pipeline(): ImagePipeline
}

class ImagePipeline {
    fun resize(maxWidth: Int, maxHeight: Int): ImagePipeline
    fun resize(imageSize: ImageSize): ImagePipeline
    fun watermark(text: String, position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT, opacity: Float = 0.5f): ImagePipeline
    fun toJpeg(quality: Float = 0.85f): ImagePipeline
    fun toPng(): ImagePipeline
    fun process(input: ByteArray): ByteArray
}

enum class WatermarkPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER }
```

### `ImageHasher` — NEW (commonLib/commonMain)

```kotlin
object ImageHasher {
    fun sha256(bytes: ByteArray): String
}
```

### `ShopifyBidirectionalSyncEngine` — ENHANCED (shopifyLib/jvmMain)

`detectChanges()` enhanced from 2 to 10+ fields: title, descriptionHtml, price, barcode, vendor, weight, tags, published, imageCount, productType

---

## 4. Utility API

```kotlin
// commonLib/jvmMain
fun ByteArray.isValidImage(): Boolean      // Check magic bytes for PNG/JPEG/WebP
fun ByteArray.imageDimensions(): Pair<Int, Int>  // Width x Height without full decode
```

---

## 5. UI Building Blocks (KVision — jsMain)

### New Column Definitions

| Component | Module | Purpose |
|-----------|--------|---------|
| `colDefShopifyCollectionRules` | shopifyLib/jsMain | Render rules as pill badges |
| `colDefFieldChange` | shopifyLib/jsMain | Render "old → new" with color coding |
| `colDefBulkSyncResult` | shopifyLib/jsMain | Summary: "✓ N · ↻ N · ✗ N" |

### New Components

| Component | Module | Purpose |
|-----------|--------|---------|
| `DiffViewerPanel` | shopifyLib/jsMain | Side-by-side ERP vs Shopify field comparison |
| `CollectionRuleEditor` | shopifyLib/jsMain | Dynamic form for adding/editing collection rules |

---

## 6. Error / Result Types

| Type | Status |
|------|--------|
| `SyncError` | Existing — unchanged |
| `BulkSyncResult` | New |
| `SimpleState` | External (fsLib) — unchanged |
| `ItemState<T>` | External (fsLib) — unchanged |

---

## 7. Versioning Policy

- **Current**: 1.3.0 → **Target**: 2.0.0
- Major bump due to breaking `SyncDiff.changedFields` type change
- **Migration**: Replace `changedFields: List<String>` usage with `List<FieldChange>` and access `.fieldName`

---

[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · **API SURFACE** · [VIEWS](VIEW_MAP.md) · PLAN · TESTS · AUDIT
