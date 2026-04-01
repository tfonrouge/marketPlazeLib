# VIEW_MAP — FullShopifySync (FEATURE)

> This library provides KVision UI building blocks, not standalone views. This document catalogs reusable components consumers import.

---

## 1. Existing Building Blocks (unchanged in v2.0.0)

| Component | Module | Type |
|-----------|--------|------|
| `colDefMarketplace*` (6) | commonLib/jsMain | Column Definitions |
| `colDefProduct*` (4) | commonLib/jsMain | Column Definitions |
| `colDefMarketplaceListing*` (3) | commonLib/jsMain | Column Definitions |
| `colDefCostBreakdown*` (5) | commonLib/jsMain | Column Definitions |
| `colDefCompetitorPrice*` (3) | commonLib/jsMain | Column Definitions |
| `filterMarketplace` | commonLib/jsMain | Off-canvas Filter |
| `filterMarketplaceListing` | commonLib/jsMain | Off-canvas Filter |
| `filterCompetitorPrice` | commonLib/jsMain | Off-canvas Filter |
| `formMarketplace` | commonLib/jsMain | Form Builder |
| `formProduct` | commonLib/jsMain | Form Builder |
| `formCompetitorPrice` | commonLib/jsMain | Form Builder |

## 2. New in v2.0.0

| Component | Module | Type | Purpose |
|-----------|--------|------|---------|
| `colDefShopifyCollectionRules` | shopifyLib/jsMain | Column Definition | Renders rules as colored badges (field=condition) |
| `colDefFieldChange` | shopifyLib/jsMain | Column Definition | Renders "old → new" with red/green color coding |
| `colDefBulkSyncResult` | shopifyLib/jsMain | Column Definition | Summary: "✓ N · ↻ N · ✗ N" |
| `DiffViewerPanel` | shopifyLib/jsMain | Component | Two-column panel: ERP vs Shopify values per field |
| `CollectionRuleEditor` | shopifyLib/jsMain | Component | Dynamic form: add/remove rules with field/relation/condition dropdowns |

> **Status**: These components are specified but not yet implemented in shopifyLib/jsMain. Consumer apps (e.g., mppDulce) provide equivalent UI directly in their view layer.

## 3. Consumer Assembly Pattern

```kotlin
// In consumer's ViewList for sync results:
override fun columnDefinitionList() = listOf(
    colDefProductSku(),
    colDefProductTitle(),
    colDefFieldChange(),        // NEW
    colDefBulkSyncResult(),     // NEW
)

// In consumer's collection editor:
override fun Container.pageItemBody() = viewFormPanel {
    CollectionRuleEditor(this)  // NEW
}
```

---

[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · **VIEWS** · PLAN · TESTS · AUDIT
