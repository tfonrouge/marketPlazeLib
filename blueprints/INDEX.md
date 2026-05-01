# Blueprints Index
**Updated**: 2026-05-01

## Active Modules & Libraries
| Name | Mode | Status | Phase | Owner | Notes |
|------|------|--------|-------|-------|-------|
| [MarketPlazeLib(MODULE)](MarketPlazeLib(MODULE)/BRIEF.md) | LIBRARY | ✅ Complete | v2.5.0 shipped | @tfonrouge | Published v2.5.0 under `com.fonrouge.marketPlazeLib` |

## Active Bridges
| Bridge | Connects | Type | Status | Expiry Condition |
|--------|----------|------|--------|-----------------|
| [ShopifyPushSync(BRIDGE)](ShopifyPushSync(BRIDGE)/BRIEF.md) | commonLib ↔ shopifyLib | Permanent | ✅ Complete (22/22; v2.5.0 mutation refresh) | — |

## Active Features
| Feature | Module | Status | Owner | Notes |
|---------|--------|--------|-------|-------|
| [FullShopifySync(FEATURE)](FullShopifySync(FEATURE)/BRIEF.md) | shopifyLib + commonLib | ✅ Complete | @tfonrouge | v2.3.0+v2.4.0+v2.5.0: collection CRUD, image pipeline, diff engine, bulk ops, smoke test, mutation deprecation cleanup |
| [MercadoLibreOrders(FEATURE)](MercadoLibreOrders(FEATURE)/BRIEF.md) | mercadoLibreLib | 🟡 In Progress | @tfonrouge | Model + abstract coll shipped (commit `7df6b3f`); TEST_PLAN + AUDIT pending |

## Deferred (blueprint only — no active development)
| Bridge | Connects | Type | Status | Reactivation Trigger |
|--------|----------|------|--------|---------------------|
| [MercadoLibreIntegration(BRIDGE)](MercadoLibreIntegration(BRIDGE)/BRIEF.md) | mercadoLibreLib ↔ ML API | Permanent | ⏸️ Deferred | Consumer commits to providing OAuth2 flow + endpoint priority list; only BRIEF.md exists, 5/6 BRIDGE artifacts pending |
| [AmazonIntegration(BRIDGE)](AmazonIntegration(BRIDGE)/BRIEF.md) | amazonLib ↔ SP-API | Permanent | ⏸️ Deferred | Consumer registers SP-API access + LWA/STS flow; only BRIEF.md exists, 5/6 BRIDGE artifacts pending |

## Deprecated
| Name | Reason | Deprecated On |
|------|--------|---------------|

## Dependency Map
```mermaid
graph LR
  MarketPlazeLib --> ShopifyPushSync
  MarketPlazeLib --> FullShopifySync
  MarketPlazeLib --> MercadoLibreOrders
  FullShopifySync --> ConsumerApps[Consumer Applications]
  ShopifyPushSync --> ConsumerApps
  MercadoLibreOrders --> ConsumerApps
```

## Status Legend
| Icon | Meaning |
|------|---------|
| 🔵 Planning | Blueprint in progress, no code yet |
| 🟡 In Progress | Active development |
| 🟢 Stable | In production, blueprint aligned |
| ⚠️ Drifted | AUDIT.md flags misalignment |
| 🔴 Blocked | Waiting on external dependency |
| ⏸️ Deferred | Blueprint exists but not staffed; awaiting reactivation trigger |
| ✅ Complete | Shipped and verified |
| 🗄️ Deprecated | No longer active |