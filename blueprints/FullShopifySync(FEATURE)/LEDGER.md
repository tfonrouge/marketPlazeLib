# Decision Ledger — FullShopifySync

| Date | Decision | Type | Rationale | Evidence required to revisit |
|------|----------|------|-----------|------------------------------|
| 2026-03-31 | Smart collection rules modeled as opaque triple `{column, relation, condition}` | APPROVED | Mirrors Shopify's `CollectionRuleInput` 1:1; passes through without validation | Shopify changes the rule schema; OR consumers ship invalid rule combinations and ask the library to validate |
| 2026-03-31 | Image processing pipeline lives in `commonLib` JVM source set, no external image library | APPROVED | `java.awt.image` is enough for resize+watermark+JPEG; avoids native deps; preserves KMP common module purity | A platform target without `java.awt` (Android, iOS, Wasm) needs image processing; OR resize quality / format support needs (WebP, AVIF) outgrows AWT |
| 2026-03-31 | `ImageHasher` uses SHA-256 for deduplication | APPROVED | Cryptographic-grade overkill for dedup, but consistent and KMP-portable via `java.security.MessageDigest` (JVM only) | KMP common needs hashing; OR image dedup performance becomes a bottleneck (then evaluate xxHash/MurmurHash) |
| 2026-03-31 | `bulkSyncToRemote` does sequential calls with configurable delay, not concurrent batched calls | APPROVED | Shopify rate limits punish concurrent abuse; sequential with delay is documented-safe | Shopify increases rate limits or exposes a bulk-mutation API; OR consumers complain about throughput |
| 2026-03-31 | `detectChanges` covers 10 fields (title, descriptionHtml, price, barcode, vendor, weight, tags, published, inventoryQuantity, productType) | APPROVED | Covers Shopify's commonly-edited fields per ERP feedback; can extend by adding to the `compare()` block | Consumers report a field that changes in ERP but does not trigger sync (then add to detector); OR Shopify adds a new product field that should be diffed |
| 2026-03-31 | `BREAKING` change to `SyncDiff.changedFields` shipped as MINOR (2.3.0), not MAJOR | APPROVED | Pre-1.0-stability mindset; type lives in shopifyLib; no consumers using `List<String>` shape were known | Project commits to strict semver from a 3.0.0 release onward (then breaking changes go in major bumps regardless of how few consumers) |
| 2026-05-01 | Skip `IMPLEMENTATION_PLAN.md` and `TEST_PLAN.md` artifacts for this FEATURE | APPROVED | SPECIFICATION + API_SURFACE were sufficient for a single-developer execution; tests live alongside code | A FEATURE blueprint needs to coordinate >1 developer; OR cathedral skill enforces FEATURE artifact set mechanically |

## Standing Rejections
_None recorded._

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [AUDIT](AUDIT.md)
