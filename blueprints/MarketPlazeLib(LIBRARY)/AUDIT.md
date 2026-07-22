# Audit — MarketPlazeLib
**Last Audit**: 2026-05-01
**Audited By**: Claude Code assisted (cathedral-premise)
**Overall Status**: ⚠️ Aligned with documented drift

## Release History Tracked Since Last Audit
| Version | Date | Highlights |
|---------|------|-----------|
| 1.0.0 | 2026-03-15 | Initial KMP library — 6 entities, pricing services, sync interfaces |
| 1.1.0 | 2026-03-16 | ShopifyPushSync(BRIDGE) — `syncToRemote()`, `IErpProductReader`, push engine |
| 1.2.0 / 2.1.0 | 2026-03 | Mexican marketplace pricing engine — dual commissions, CargoFijoTier, EnvioTier, EnvioDiscount, ShippingCalculator (volumetric) |
| 1.3.0 | 2026-03-27 | fsLib 3.2.0-SNAPSHOT, KVision 9.4.0-SNAPSHOT |
| 2.2.0 | 2026-03-31 | Versioning fix; stable fsLib 3.2.0 / KVision 9.5.0 |
| 2.3.0 | 2026-03-31 | FullShopifySync — collection CRUD, image pipeline (`ImageProcessor`/`ImageHasher`), 10-field diff engine, bulk ops, `readInventoryByLocation()` |
| 2.4.0 | 2026-04-01 | `getProduct()` query, `ShopifySmokeTest` round-trip |
| 2.5.0 | 2026-05-01 | `MarketplaceType` constants object; build cleanup (catalog centralization, Dokka V2, dependency dedup, deprecated Shopify mutations replaced) |
| 2.6.1 | 2026-06 | MercadoLibreOrders(FEATURE) — `MercadoLibreOrder`, `MlOrderReview`, `mlShipment` (lookup-only FULL/Normal) |
| 2.7.0 | 2026-06-24 | **Toolchain migration** — fsLib 6.0.0, Kotlin 2.4.0, KVision 9.6.0, kilua-rpc 0.0.45, KSP 2.3.9, Apollo 5.0.0, serialization 1.11.0; **JDK 25 / JVM target 25** (published bytecode major 69). Driven by mppErsaPack's fsLib-6 migration; eliminates the stale fsLib 3.2.0 transitive request. `allTests` green (77 JVM tests); downstream mppErsaPack build gate re-verified green. See LEDGER 2026-06-24. |

## Drift Log (resolved 2026-05-01)
| Artifact | Section | Blueprint Said | Reality | Severity | Action Taken |
|----------|---------|----------------|---------|----------|--------------|
| API_SURFACE.md | `Marketplace` data class | 8 fields (no Mexican pricing fields) | 14 fields incl. `commissionRateClassic/Premium`, `cargoFijoTiers`, `envioTiers`, `envioDiscounts`, `isDefault`, `minimumPrice` | High | Updated 2026-05-01 |
| API_SURFACE.md | `IErpProductReader` | 4 methods | 5 methods (`readInventoryByLocation` added v2.3.0) | Medium | Updated 2026-05-01 |
| API_SURFACE.md | New types | `MarketplaceType` constants object missing | Added in v2.5.0 | Medium | Documented 2026-05-01 |
| BRIEF.md | Target Go-Live | "v1.0.0 — shipped 2026-03-15" — frozen | Project at v2.5.0 | Low | Release History added in this AUDIT |
| AUDIT.md | Last Audit | 2026-03-16 | Six weeks and four releases stale | High | Resolved by this re-run |

## Outstanding Drift / Open Questions
| Item | Severity | Notes |
|------|----------|-------|
| Directory suffix `(MODULE)` vs LIBRARY mode | Low | INDEX.md correctly labels Mode column as LIBRARY; suffix is convention-only and renaming would break inbound links. Decision logged in `LEDGER.md`. |
| `MarketplaceType` is `object`-of-`const val` rather than `enum class` | Medium | Trade-off chosen consciously to avoid breaking `Marketplace.type: String` in v2.5.0. Falsification condition recorded in `LEDGER.md`. |

## Audit Checklist
- [x] Every FR in SPECIFICATION.md has a corresponding implementation or explicit deferral
- [x] API_SURFACE.md matches current public API signatures (KMP — JVM + JS) (refreshed 2026-05-01)
- [x] VIEW_MAP.md matches current UI building blocks (KVision column defs, filter views, form builders)
- [x] TRACEABILITY_MATRIX.md status reflects actual completion through v2.5.0
- [x] BRIEF.md scope still matches what was built — Mexican pricing engine and FullShopifySync now reflected via Release History above
- [x] LEDGER.md created with falsification conditions for standing approvals (2026-05-01)

## Notes
- Tests pass; one unrelated compiler warning at `PricingServiceTest.kt:359` (Unnecessary safe call on `CostBreakdown`) — cleanup candidate, not blocking.
- No `[SPIKE]` tags found in production code.
- `CLAUDE.md` cathedral premise block added 2026-05-01.

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [SPEC](SPECIFICATION.md) · [FLOWCHART](FLOWCHART.md) · [API SURFACE](API_SURFACE.md) · [VIEWS](VIEW_MAP.md) · [PLAN](IMPLEMENTATION_PLAN.md) · [TESTS](TEST_PLAN.md) · [MATRIX](TRACEABILITY_MATRIX.md) · [LEDGER](LEDGER.md) · **AUDIT**
