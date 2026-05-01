# Cathedral Audit Report
> Generated: 2026-05-01
> Premise: cathedral (informal — not yet declared in CLAUDE.md)
> Skill: business-blueprint-workflow
> Blueprints scanned: blueprints/

## Summary
- Blueprints scanned: 6
- Passing: 1
- Warnings: 5
- Violations: 4
- Critical: 0

The repo has working blueprints and disciplined commits, but premise governance is not formally adopted (no `## Premise: cathedral` block in `CLAUDE.md`, no `LEDGER.md` per blueprint), and the design artifacts have drifted noticeably behind the code that has shipped between v2.0.0 and v2.5.0. The drift is concentrated in three places: (1) the `MarketPlazeLib(MODULE)` AUDIT.md hasn't been re-run since v1.0.0; (2) `ShopifyPushSync(BRIDGE)` SERVICE_CONTRACTS still describes the deprecated `productCreateMedia/productDeleteMedia` mutations replaced in v2.5.0; (3) `FullShopifySync(FEATURE)` is missing AUDIT.md and IMPLEMENTATION_PLAN/TEST_PLAN despite being marked ✅ Complete in INDEX.md.

---

## Cross-cutting Findings

### [Premise] Cathedral premise not formally adopted — Warning
`CLAUDE.md` has no `## Premise: cathedral` block, no `Blueprint skill:` declaration, and no per-blueprint `LEDGER.md` files exist. The repo follows the spirit of the premise (blueprints, BRIEF/AUDIT artifacts, conventional commits) but lacks the explicit configuration the premise requires. **Action:** add the config block to `CLAUDE.md` and seed each active blueprint with a `LEDGER.md`.

### [Documentation] CLAUDE.md dependency facts are stale — Warning
`CLAUDE.md` lines 53, 56 state fsLib `3.1.1` and Apollo GraphQL `4.3.3`. Actual `gradle/libs.versions.toml` has fsLib `3.2.0` and Apollo `4.4.2`. Authoritative source is the catalog; the prose drifts on every dependency bump. **Action:** either remove the version numbers from `CLAUDE.md` (point to the catalog) or add a CI check that rejects mismatch.

### [Decision Ledger] No `LEDGER.md` files exist — Warning
The premise mandates a Decision Ledger per blueprint with falsification conditions on every APPROVED entry. Without it, both rejection amnesia and approval calcification are possible. The "No backward compatibility" decision in `MarketPlazeLib(MODULE)/BRIEF.md` and the `Money = Long cents` decision are exactly the kind of approvals that should carry falsification conditions. **Action:** create `LEDGER.md` per blueprint, retroactively populated with the decisions already documented in BRIEF "Key Decisions" sections.

---

## Per-blueprint Results

### MarketPlazeLib (MODULE / actually LIBRARY)
**Status:** ⚠️ Warnings + ❌ Violations
**Mode:** declared `(MODULE)` but artifacts and intent describe a LIBRARY

#### Violations
- **[Established Patterns / Mode mismatch]** Directory suffix `(MODULE)` but the artifact is a multi-module Maven-published Kotlin library. Per cathedral-business.md, LIBRARY mode requires `BRIEF, ARCHITECTURE, PUBLIC_API, IMPLEMENTATION_ORDER, AUDIT`. This blueprint instead carries `SPECIFICATION, FLOWCHART, API_SURFACE, VIEW_MAP, IMPLEMENTATION_PLAN, TEST_PLAN, TRACEABILITY_MATRIX, AUDIT` — closer to MODULE template. Either rename suffix or migrate to the LIBRARY artifact set. INDEX.md already labels its Mode column "LIBRARY," confirming the mismatch is in the directory name and artifact list. — `MarketPlazeLib(MODULE)/`
- **[Blueprint Alignment]** AUDIT.md last run 2026-03-16, claims status ✅ Aligned at v1.0.0, but versions 1.1.0 through 2.5.0 have shipped since. AUDIT must be re-run; the current Drift Log shows zero rows for any of: ShopifyPushSync (v1.1.0), Mexican pricing engine (v1.2.0/v2.1.0), FullShopifySync feature additions (v2.3.0), `getProduct()` query (v2.4.0), `MarketplaceType` constants (v2.5.0). — `MarketPlazeLib(MODULE)/AUDIT.md:2`

#### Warnings
- **[Clean Abstractions / Drift]** API_SURFACE.md `Marketplace` signature (line 12) is missing seven fields present in the actual model: `commissionRateClassic`, `commissionRatePremium`, `cargoFijoTiers`, `envioTiers`, `envioDiscounts`, `isDefault`, `minimumPrice`. — `MarketPlazeLib(MODULE)/API_SURFACE.md:12`
- **[Clean Abstractions / Drift]** API_SURFACE.md `IErpProductReader` (lines 84–89) shows 4 methods; actual interface (commonLib/sync/IErpProductReader.kt) has 5 — `readInventoryByLocation()` was added in v2.3.0 with default empty map. — `MarketPlazeLib(MODULE)/API_SURFACE.md:84-89`
- **[Solve General Problem]** BRIEF.md "Key Decisions" lists `Connection config: Generic Map<String, String> per marketplace`. v2.5.0 just shipped a `MarketplaceType` constants object that hints we are inching toward typed marketplace identifiers without committing. Either the generic-map decision still holds (then say so explicitly in a LEDGER entry with falsification condition: "if the constants object grows beyond 3 marketplaces, revisit") or the design is migrating and BRIEF should reflect that.

#### Notes
- BRIEF.md "Target Go-Live" still says "v1.0.0 — shipped 2026-03-15"; the project is now at v2.5.0. Move the v1.0.0 milestone into a "Release History" section instead of leaving the field frozen.

---

### ShopifyPushSync (BRIDGE)
**Status:** ❌ Violations
**Mode:** BRIDGE (Permanent)

#### Violations
- **[Blueprint Alignment / Critical-tier drift]** SERVICE_CONTRACTS.md line 105 still documents image mutations as `stagedUploadsCreate → HTTP POST → productCreateMedia / productDeleteMedia / productReorderMedia`. Actual implementation in `shopifyLib/.../ShopifyPushEngine.kt` uses `productUpdate` (with media param) and `fileDelete` — replaced in commit `5408d64`, released as part of v2.5.0. The blueprint contract no longer matches the code. — `ShopifyPushSync(BRIDGE)/SERVICE_CONTRACTS.md:105`
- **[Clean Abstractions / Drift]** SERVICE_CONTRACTS.md `IErpProductReader` (lines 28–34) lists 4 methods but the production interface has 5 (`readInventoryByLocation`). Same drift as in MODULE/API_SURFACE.md — single source of truth violation. — `ShopifyPushSync(BRIDGE)/SERVICE_CONTRACTS.md:28-34`

#### Warnings
- **[AUDIT currency]** AUDIT.md last run 2026-03-16 reports "✅ Complete (22/22)" — predates v2.3.0 (FullShopifySync), v2.4.0 (getProduct), and v2.5.0 (Shopify mutation replacement). The AUDIT note at line 29 still describes the now-replaced `ProductCreateMediaMutation` / `ProductDeleteMediaMutation` as the current implementation. **Action:** re-run AUDIT, log the v2.5.0 mutation swap as a Drift Log entry resolved by updating SERVICE_CONTRACTS.md.
- **[Approval Calcification]** BRIEF "Lifecycle Type: Permanent" with no falsification condition. Permanent bridges still benefit from a documented "this becomes load-bearing if X" condition. Suggested falsification: "revisit if Shopify deprecates Admin GraphQL or marketPlazeLib gains a non-Shopify push target."

#### Notes
- All 10 service methods are present in `IShopifyPushService`; the contract structurally matches; only the underlying GraphQL mutation names drifted.

---

### FullShopifySync (FEATURE)
**Status:** ❌ Violations
**Mode:** FEATURE

#### Violations
- **[AUDIT currency / Blueprint completeness]** INDEX.md line 17 claims this feature is "✅ Complete" with v2.3.0+v2.4.0 deliverables. Blueprint directory contains `BRIEF, SPECIFICATION, FLOWCHART, API_SURFACE, VIEW_MAP` — **no AUDIT.md, no IMPLEMENTATION_PLAN.md, no TEST_PLAN.md**. Cathedral-business mandates AUDIT.md for every blueprint. — `FullShopifySync(FEATURE)/`
- **[Documentation drift]** API_SURFACE.md line 3–4 reads "Library: `marketPlazeLib` v2.0.0 / Published Coordinates: ...:2.0.0". Actual ship was v2.3.0–v2.5.0. Version target should either be "v2.3.0+ (shipped)" or split per release. — `FullShopifySync(FEATURE)/API_SURFACE.md:3-4`

#### Warnings
- **[Spike hygiene / not violated, but unverifiable]** No `[SPIKE]` tags found in git log or source. Per audit policy this is a positive — but with the missing AUDIT.md there's no recorded confirmation that the bulk-sync, image pipeline, and diff-engine work was treated as construction rather than spike.

#### Notes
- All advertised deliverables exist in code: `BulkSyncResult`, `FieldChange`, `ImageProcessor`, `ImageHasher`, `bulkSyncToRemote`, expanded `detectChanges` (10+ fields). The drift here is paperwork, not implementation.

---

### MercadoLibreOrders (FEATURE)
**Status:** ⚠️ Warnings
**Mode:** FEATURE
**Phase:** 🔵 Planning

#### Warnings
- **[Blueprint completeness]** BRIEF, SPECIFICATION, API_CONTRACT, IMPLEMENTATION_PLAN exist; no FLOWCHART, no TEST_PLAN, no AUDIT. Acceptable for Planning phase per cathedral-core ("If no code exists yet for a blueprint, skip the drift check"). However, commit `7df6b3f` is titled "feat: MercadoLibreOrder model + abstract collection for order sync" — implementation has begun. Reconcile INDEX.md status (currently 🔵 Planning) with the partial implementation: bump to 🟡 In Progress and add a TEST_PLAN before further code.
- **[Documentation drift]** BRIEF.md "Target: MarketPlazeLib v1.4.0+" — there is no v1.4.0 (project skipped from v1.3.0 to v2.1.0/v2.2.0). Version target must be updated.

#### Notes
- "Pattern Reference" table (Product → Order parallel) is exactly the kind of established-pattern adherence the premise wants — flag as positive.

---

### MercadoLibreIntegration (BRIDGE)
**Status:** ⚠️ Warnings
**Mode:** BRIDGE (Permanent), Planning

#### Warnings
- **[Blueprint completeness / BRIDGE coherence]** Only BRIEF.md exists. BRIDGE mode in cathedral-business requires `BRIEF, ENTITY_DESCRIPTOR, SERVICE_CONTRACTS, VIEW_MAP, IMPLEMENTATION_ORDER, AUDIT`. Five of six are missing. The bridge has been in Planning state since at least 2026-03-16 (>6 weeks); per the escalation trigger "blueprint in ACTIVE >4 weeks without AUDIT.md updates" this warrants reclassification to "Deferred" or completing the artifact set.
- **[Documentation drift]** BRIEF.md "Library Version Target: MarketPlazeLib v1.2.0+" — current version is v2.5.0; this target is meaningless. Update or delete.

---

### AmazonIntegration (BRIDGE)
**Status:** ⚠️ Warnings
**Mode:** BRIDGE (Permanent), Planning

#### Warnings
- Same as MercadoLibreIntegration: only BRIEF.md exists; 5/6 BRIDGE artifacts missing; "Target: TBD"; "Library Version Target: v1.2.0+" stale.

---

## Principle Alignment Snapshot

| Principle | Status | Most relevant finding |
|---|---|---|
| 1. Correctness First | ✅ | No `[SPIKE]` tags in production code; tests pass; one `Unnecessary safe call` compiler warning in `PricingServiceTest.kt:359` (not a violation, cleanup candidate). |
| 2. Solve the General Problem | ⚠️ | New `MarketplaceType` is `object` with `const val String` — covers only 3 marketplaces with no compile-time enforcement. See v2.5.0 critique above. |
| 3. Clean Abstractions | ❌ | API_SURFACE / SERVICE_CONTRACTS describe interfaces that do not match shipped code (`Marketplace` fields, `IErpProductReader`, image mutations). |
| 4. Established Patterns Over Novel Invention | ✅ | Apollo GraphQL, KMP source-set conventions, value class for Money, abstract `fetchProducts()` pattern reused across ML/Amazon. |
| 5. Incremental Discipline | ⚠️ | Increments are small and reasoned, but no LEDGER.md exists; no falsification conditions on standing approvals; AUDIT.md updates are months behind shipped code. |

---

## Recommended Next Actions (ordered)

1. **Refresh AUDIT.md** for `MarketPlazeLib(MODULE)` and `ShopifyPushSync(BRIDGE)` to reflect everything shipped through v2.5.0; reconcile drift log.
2. **Create `FullShopifySync(FEATURE)/AUDIT.md`** since INDEX claims it Complete.
3. **Update `ShopifyPushSync(BRIDGE)/SERVICE_CONTRACTS.md`** lines 28–34 (IErpProductReader 5 methods) and line 105 (productUpdate / fileDelete instead of productCreateMedia / productDeleteMedia).
4. **Update `MarketPlazeLib(MODULE)/API_SURFACE.md`** Marketplace signature and IErpProductReader.
5. **Adopt the cathedral premise formally** — add `## Premise: cathedral` block to `CLAUDE.md` per the template in `cathedral-core.md`.
6. **Create `LEDGER.md`** per active blueprint, with falsification conditions on the standing approvals (Money = cents, generic connectionConfig Map, Permanent bridge lifecycle).
7. **Decide on `MarketplaceType`**: enum vs const-object, then back-fill internal call sites and document the choice in the new LEDGER.
8. **Reclassify `MercadoLibreIntegration` and `AmazonIntegration` BRIDGE blueprints** — either complete the missing 5/6 artifacts or move to Deferred so they don't appear "Active" in INDEX.
9. **Strip stale version numbers from CLAUDE.md** prose (or wire a check).
