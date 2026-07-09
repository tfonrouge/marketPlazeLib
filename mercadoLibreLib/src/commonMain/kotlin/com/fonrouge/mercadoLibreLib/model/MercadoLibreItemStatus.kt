package com.fonrouge.mercadoLibreLib.model

import kotlinx.serialization.Serializable

/**
 * The subset of MercadoLibre item `status` values a seller can *set* via `PUT /items/{id}`.
 *
 * ML also reports other read-only statuses on the pull side (e.g. `under_review`, `inactive`) —
 * those are surfaced as raw strings on [MercadoLibreProduct.status] and are NOT settable here.
 *
 * Semantics (confirm against ML docs before relaxing):
 * - [ACTIVE]  — listing visible to buyers. `PAUSED` → `ACTIVE` reactivates.
 * - [PAUSED]  — temporarily hidden; reversible back to `ACTIVE`.
 * - [CLOSED]  — finished/retired. Treated as terminal by ML (may not be reopenable) — the UI
 *               must confirm before setting this.
 */
@Serializable
enum class MercadoLibreItemStatus(val apiValue: String) {
    ACTIVE("active"),
    PAUSED("paused"),
    CLOSED("closed"),
}
