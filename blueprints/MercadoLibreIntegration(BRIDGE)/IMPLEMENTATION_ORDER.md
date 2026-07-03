# Implementation Order — MercadoLibreIntegration(BRIDGE)

**Updated**: 2026-07-03
**Status**: 9/9 library-side tasks complete. Consumer wiring (mppErsaPack) is a separate, not-yet-started effort — tracked in `mppErsaPack/blueprints/MarketPlace(MODULE)`, not in this checklist.

## Checklist

### Layer 1 — Build Configuration
- [x] B4-01 · Add `ktor-client-core`, `ktor-client-cio`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` aliases to `gradle/libs.versions.toml` (reusing existing `ktor = "3.4.2"` version ref)
- [x] B4-02 · Wire those as `jvmMain` dependencies in `mercadoLibreLib/build.gradle.kts`
- [x] B4-03 · Add `ktor-client-mock` as a `jvmTest`-only dependency

### Layer 2 — Interface
- [x] B4-04 · Define `IMercadoLibrePushService` (commonMain) — `updateAvailableQuantity(marketplace, itemId, quantity): SimpleState`

### Layer 3 — Implementation
- [x] B4-05 · Implement `MercadoLibrePushEngine` (jvmMain) — Ktor CIO client, `PUT /items/{id}`, 401 surfaced without retry, ML error message extraction on failure
- [x] B4-06 · Make the engine testable — optional injected `HttpClientEngine` constructor param (defaults to real CIO in production)

### Layer 4 — Validation
- [x] B4-07 · `MercadoLibrePushEngineTest` (jvmTest, `MockEngine`): success path (verifies URL, Bearer header, JSON body), 401 path, generic API error path (verifies ML's own error message surfaces), missing-accessToken fast-fail path
- [x] B4-08 · `./gradlew :mercadoLibreLib:jvmTest` green (6/6, incl. 2 added after an adversarial audit found a body-double-read risk & status-code loss on non-JSON errors — see LEDGER D8)
- [x] B4-09 · `./gradlew build` (full repo, all modules) green — zero regressions

### Layer 5 — Not in this bridge (tracked elsewhere)
- [ ] Wire a real consumer in mppErsaPack (`pushInventoryToMercadoLibre` calling `IMercadoLibrePushService`) — **blocks this bridge reaching `STABLE`**, see LEDGER D5. Tracked in `mppErsaPack/blueprints/MarketPlace(MODULE)`.
- [ ] Resolve SKU/Clave → MercadoLibre itemId matching in the consumer (mppErsaPack found `sellerSku` empty on 100% of synced ML products this session — a prerequisite for any consumer to call `updateAvailableQuantity` correctly)
- [ ] Migrate the existing read-side `MercadoLibreApiClient` (fetchProducts/fetchOrders/OAuth) from mppErsaPack into mercadoLibreLib — separate, higher-risk migration, explicitly out of scope for this bridge (see BRIEF)

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · **ORDER** · [AUDIT](AUDIT.md) · [LEDGER](LEDGER.md)
