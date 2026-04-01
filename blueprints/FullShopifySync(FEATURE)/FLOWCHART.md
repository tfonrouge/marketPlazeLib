# FLOWCHART — FullShopifySync (FEATURE)

## 1. Core Processing Flow — Bidirectional Sync with Enhanced Diff

```mermaid
flowchart TD
    Start([Sync Triggered]) --> ReadERP[IErpProductReader.readProducts]
    ReadERP --> ReadShopify[IShopifyColl.syncShopifyToLocal]
    ReadShopify --> Reconcile[ShopifyBidirectionalSyncEngine.reconcile]
    Reconcile --> DetectChanges[detectChanges - 10+ field comparison]
    DetectChanges --> Classify{Classify each product}
    Classify -->|No Shopify match| ToCreate[SyncReport.toCreate]
    Classify -->|Fields differ| ToUpdate[SyncReport.toUpdate with FieldChange list]
    Classify -->|Fields match| InSync[SyncReport.inSync]
    Classify -->|No ERP match| Orphaned[SyncReport.orphaned]
    ToCreate --> BulkCreate[bulkCreateProducts - batched]
    ToUpdate --> BulkUpdate[bulkUpdateProducts - batched]
    BulkCreate --> Report[BulkSyncResult]
    BulkUpdate --> Report
    InSync --> Report
    Orphaned --> Report
```

## 2. Image Processing Pipeline

```mermaid
flowchart TD
    Input([Image bytes from ERP]) --> Validate{Size < 20MB?}
    Validate -->|No| Reject[Reject: exceeds Shopify limit]
    Validate -->|Yes| Hash[SHA-256 hash]
    Hash --> DedupCheck{Already uploaded?}
    DedupCheck -->|Yes| Skip[Skip duplicate]
    DedupCheck -->|No| Pipeline[ImageProcessor.pipeline]
    Pipeline --> Resize[resize maxWidth, maxHeight]
    Resize --> Watermark{Watermark enabled?}
    Watermark -->|Yes| AddWatermark[watermark text, position, opacity]
    Watermark -->|No| Format
    AddWatermark --> Format[toJpeg / toPng]
    Format --> StagedUpload[stagedUploadsCreate mutation]
    StagedUpload --> Upload[HTTP PUT to staged URL]
    Upload --> Attach[productCreateMedia mutation]
    Attach --> Done([Image attached to product])
```

## 3. Smart Collection Sync Flow

```mermaid
flowchart TD
    Start([Collection sync triggered]) --> ReadLocal[Read collection definitions from consumer]
    ReadLocal --> ReadShopify[Query Shopify collections via GraphQL]
    ReadShopify --> Compare{Compare local vs Shopify}
    Compare -->|New locally| Create[createCollection mutation with rules]
    Compare -->|Changed| Update[updateCollection mutation]
    Compare -->|Deleted locally| Delete[deleteCollection mutation]
    Compare -->|In sync| Skip[No action]
    Create --> Done([Sync complete])
    Update --> Done
    Delete --> Done
    Skip --> Done
```

## 4. Consumer App Integration Sequence

```mermaid
sequenceDiagram
    participant App as Consumer App
    participant ERP as IErpProductReader
    participant Engine as ShopifyBidirectionalSyncEngine
    participant Push as ShopifyPushEngine
    participant GQL as Shopify GraphQL API

    App->>ERP: readProducts()
    ERP-->>Engine: List<Product>
    App->>Engine: reconcile(marketplace)
    Engine->>GQL: GetProductsQuery (paginated)
    GQL-->>Engine: ShopifyProduct list
    Engine->>Engine: detectChanges(10+ fields)
    Engine-->>App: SyncReport

    App->>Engine: syncToRemote(marketplace)
    Engine->>Push: bulkCreateProducts(toCreate)
    Push->>GQL: ProductCreate mutations (batched, 500ms)
    Engine->>Push: bulkUpdateProducts(toUpdate)
    Push->>GQL: ProductUpdate mutations (batched, 500ms)
    Engine-->>App: BulkSyncResult
```

## 5. Smoke Test — Round-Trip Verification

```mermaid
sequenceDiagram
    participant Test as ShopifySmokeTest
    participant Push as IShopifyPushService
    participant GQL as Shopify GraphQL

    Test->>Push: getProduct(marketplace, shopifyId)
    Push->>GQL: GetProductDetailQuery
    GQL-->>Push: Product (original title)
    Push-->>Test: ItemState<Product> — capture originalTitle

    Note over Test: testTitle = originalTitle + " [SMOKE TEST]"

    Test->>Push: updateProduct(marketplace, modifiedProduct, shopifyId)
    Push->>GQL: ProductUpdateMutation
    GQL-->>Push: OK
    Push-->>Test: SimpleState OK

    Test->>Push: getProduct(marketplace, shopifyId)
    Push->>GQL: GetProductDetailQuery
    GQL-->>Push: Product (verify testTitle)
    Push-->>Test: updateVerified = (title == testTitle)

    Note over Test: try/finally — always revert

    Test->>Push: updateProduct(marketplace, originalProduct, shopifyId)
    Push->>GQL: ProductUpdateMutation
    GQL-->>Push: OK

    Test->>Push: getProduct(marketplace, shopifyId)
    Push->>GQL: GetProductDetailQuery
    GQL-->>Push: Product (verify originalTitle)
    Push-->>Test: revertVerified = (title == originalTitle)

    Test-->>Test: Return SmokeTestResult
```

## 6. Error Handling

```mermaid
flowchart TD
    Op([Shopify API call]) --> Try{Try}
    Try -->|GraphQL userErrors| SyncError[Create SyncError]
    Try -->|Network error| Retry{Retries < 3?}
    Retry -->|Yes| Backoff[Exponential backoff: 1s, 2s, 4s]
    Backoff --> Try
    Retry -->|No| Fail[Return error]
    Try -->|Rate limited 429| Wait[Wait 1 second]
    Wait --> Try
    Try -->|Success| OK[Return SimpleState OK]
    SyncError --> Accumulate[Add to BulkSyncResult.errors]
    Accumulate --> Continue[Continue next item]
```

---

[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · **FLOWCHART** · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · PLAN · TESTS · AUDIT
