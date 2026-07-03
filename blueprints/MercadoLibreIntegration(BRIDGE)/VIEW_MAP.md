# MercadoLibreIntegration — View Map

## New Views

None. This bridge is backend-only — a library service (`IMercadoLibrePushService`) with no UI surface of its own, matching the `ShopifyPushSync(BRIDGE)` precedent (`IShopifyPushService` also ships with no library-side views; any admin UI for triggering a push belongs to the consumer app).

## Modified Views

None.

## Navigation Changes

None.

## Shared Components

None.

## Note for consumers

If a consuming app wants an operator-facing "sync stock to MercadoLibre" action (button, dashboard, log view), that UI is the consumer's responsibility to build against `IMercadoLibrePushService`, the same way mppErsaPack would build its own view against this contract — out of scope for this library-side bridge.

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · [ENTITY](ENTITY_DESCRIPTOR.md) · [CONTRACTS](SERVICE_CONTRACTS.md) · **VIEWS** · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md) · [LEDGER](LEDGER.md)
