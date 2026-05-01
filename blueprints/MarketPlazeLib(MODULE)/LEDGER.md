# Decision Ledger — MarketPlazeLib

> Symmetric record of design decisions. Every APPROVED entry carries a falsification condition: the evidence that would force a revisit. Every REJECTED entry stays standing until explicitly justified for re-litigation.

| Date | Decision | Type | Rationale | Evidence required to revisit |
|------|----------|------|-----------|------------------------------|
| 2026-03-15 | `Money` is `@JvmInline value class` over `Long` (cents) | APPROVED | Exact arithmetic; KMP-safe (no `BigDecimal` on JS); zero allocation overhead | A currency or marketplace requires sub-cent precision (e.g., crypto, fuel pricing); OR multi-currency arithmetic becomes load-bearing and breaks the implicit "single currency per calc" assumption; OR users report rounding drift in tax/commission stacks |
| 2026-03-15 | Generic `connectionConfig: Map<String, String>` per Marketplace | APPROVED | Avoids per-marketplace credential schemas; consumers serialize whatever they need | A marketplace requires non-string credentials (binary tokens, certificates) OR a security review demands typed credential validation per type |
| 2026-03-15 | KMP — JVM + JS (ES2015, IR) only | APPROVED | Two consumer surfaces; native/Wasm not requested by mppErsaPack | A consumer requests iOS/Android native or Wasm target (then evaluate cost vs. fork) |
| 2026-03-15 | Five-module Gradle layout (`commonLib`, `shopifyLib`, `mercadoLibreLib`, `amazonLib`, `samples`) | APPROVED | Per-marketplace optional dependency; sample stays runnable independently | Module count exceeds 8 OR consumers complain about coordinate sprawl on Maven Central |
| 2026-03-15 | No backward compatibility with pre-1.0 versions | APPROVED | Pre-release; clean break preferred over carrying legacy shapes | Project hits a major (3.x) version: from then on, semver compatibility is mandatory |
| 2026-03-31 | `SyncDiff.changedFields` as `List<FieldChange>` (replacing `List<String>`) — BREAKING in v2.3.0 | APPROVED | String-only diff is unusable for downstream "what changed from what" reporting | Consumers report needing per-field old/new value reconstruction beyond `FieldChange`'s 3-field shape (e.g., typed values, structured deltas) |
| 2026-05-01 | `MarketplaceType` is `object`-with-`const val String`, NOT `enum class` | APPROVED | Avoids breaking `Marketplace.type: String` field in v2.5.0; gives consumers a typo-free reference set; defers the type-safety upgrade to a future major | (a) A 4th marketplace type is added without corresponding constant, OR (b) a typo bug in `type` reaches production, OR (c) library hits 3.0.0 and we want exhaustive `when` enforcement. Any of these → migrate `Marketplace.type` to enum, deprecate the const-object. |
| 2026-05-01 | Directory name `MarketPlazeLib(MODULE)` retained despite content matching LIBRARY mode | APPROVED | Renaming would break 12+ inbound markdown links across artifacts; mode is correctly identified in INDEX.md | Cathedral audit tooling enforces directory-suffix-to-mode mapping (mechanical), or contributor confusion is reported |

## Standing Rejections
_None recorded._

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [AUDIT](AUDIT.md)
