# MercadoLibreIntegration — Entity Descriptor

## Entity Overview

No new persisted entity. This bridge adds a **service operation** (`IMercadoLibrePushService.updateAvailableQuantity`) over the existing `Marketplace` entity — it does not introduce a new `BaseDoc`, container, or collection. `MercadoLibreProduct` (existing entity, pull-side) is unchanged.

The state machine below models the *operation lifecycle* of a single push call — not an entity's persisted state — since there is no entity to track.

---

## State Machine: Push Operation Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Requested: consumer calls updateAvailableQuantity(marketplace, itemId, quantity)
    Requested --> MissingToken: connectionConfig has no accessToken
    MissingToken --> [*]: SimpleState(hasError=true) — fails fast, no HTTP call made

    Requested --> InFlight: accessToken present, PUT dispatched
    InFlight --> Success: HTTP 2xx
    Success --> [*]: SimpleState(hasError=false)

    InFlight --> AuthExpired: HTTP 401
    AuthExpired --> [*]: SimpleState(hasError=true) — caller must refresh + retry, engine does not retry itself

    InFlight --> ApiError: HTTP 4xx/5xx (not 401)
    ApiError --> [*]: SimpleState(hasError=true, msgError=ML error message)

    InFlight --> NetworkError: exception (timeout, connection failure)
    NetworkError --> [*]: SimpleState(hasError=true, msgError=exception message)
```

## Business Rules

- BR-01: The system SHALL update a single MercadoLibre item's `available_quantity` via `PUT https://api.mercadolibre.com/items/{itemId}` — the global API base, not `.com.mx` (consistent with the consumer's existing read-side client and its documented rationale for rejecting the regional host).
- BR-02: The system SHALL authenticate with `Bearer {accessToken}` sourced from `Marketplace.connectionConfig["accessToken"]`. The system SHALL NOT attempt to refresh an expired token itself.
- BR-03: On HTTP 401 the system SHALL return a `SimpleState` with `hasError=true` and a message identifying it as an auth failure, without retrying. Retry-after-refresh is the caller's responsibility.
- BR-04: On any other non-2xx response the system SHALL surface MercadoLibre's own error message (`message` or `error` field from the JSON error body) in `SimpleState.msgError`, not just the HTTP status code.
- BR-05: The system SHALL NOT perform partial/batched updates — MercadoLibre's REST API has no documented bulk stock-update endpoint (unlike Shopify's GraphQL `inventorySetQuantities`); each call updates exactly one item. Callers needing to update N items make N calls.
- BR-06: The system SHALL close the underlying HTTP client after every call (no connection reuse across calls) — matches the existing `MercadoLibreApiClient` per-call client lifecycle in mppErsaPack.

## Data Model

No new fields on any entity. Reuses:

| Entity | Field | Used as |
|--------|-------|---------|
| `Marketplace` | `connectionConfig["accessToken"]` | Bearer token for the PUT request |
| — | `itemId: String` (method parameter, not a field) | MercadoLibre item id (e.g. `MLM2801064685`) — caller is responsible for resolving this from whatever SKU/Clave matching scheme it uses; out of scope for this bridge |
| — | `quantity: Int` (method parameter) | New `available_quantity` value; caller computes this (e.g. from ERP stock minus reservations) |

---

## Integration Flow

```mermaid
sequenceDiagram
    participant App as Consumer App (e.g. mppErsaPack)
    participant Engine as MercadoLibrePushEngine
    participant ML as MercadoLibre REST API

    App->>App: resolve itemId for the product being updated (caller-owned matching)
    App->>App: compute quantity (e.g. ERP stock - reservations)
    App->>Engine: updateAvailableQuantity(marketplace, itemId, quantity)
    Engine->>Engine: read accessToken from marketplace.connectionConfig
    alt accessToken missing
        Engine-->>App: SimpleState(hasError=true, "Missing accessToken")
    else accessToken present
        Engine->>ML: PUT /items/{itemId} {"available_quantity": quantity}
        alt 2xx
            ML-->>Engine: 200 OK
            Engine-->>App: SimpleState(hasError=false)
        else 401
            ML-->>Engine: 401 Unauthorized
            Engine-->>App: SimpleState(hasError=true, "401 ... refresh required")
            App->>App: (caller's responsibility) refresh token, retry
        else other error
            ML-->>Engine: 4xx/5xx + error JSON
            Engine-->>App: SimpleState(hasError=true, msgError=ML message)
        end
    end
```

## Actor Matrix

| Actor | Call updateAvailableQuantity | Resolve itemId | Refresh expired token |
|-------|------------------------------|-----------------|------------------------|
| Consumer App (mppErsaPack orchestration) | Trigger | Owns (out of scope here) | Owns (out of scope here) |
| `MercadoLibrePushEngine` | Execute | — | — |
| MercadoLibre REST API | Respond | — | — |

---
[← Index](../INDEX.md) · [BRIEF](BRIEF.md) · **ENTITY** · [CONTRACTS](SERVICE_CONTRACTS.md) · [VIEWS](VIEW_MAP.md) · [ORDER](IMPLEMENTATION_ORDER.md) · [AUDIT](AUDIT.md) · [LEDGER](LEDGER.md)
