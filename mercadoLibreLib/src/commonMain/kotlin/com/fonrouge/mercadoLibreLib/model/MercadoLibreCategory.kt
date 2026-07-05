package com.fonrouge.mercadoLibreLib.model

import kotlinx.serialization.Serializable

/**
 * A category candidate suggested by ML's `domain_discovery/search` for a free-text query.
 * Deliberately not authoritative on its own — verified empirically (mppErsaPack MarketPlace(MODULE)
 * LEDGER D62) that this prediction can be wrong for ambiguous queries (a real product title matched
 * a furniture category instead of the correct one) — callers must let a human confirm, never
 * auto-apply the top result.
 */
@Serializable
data class MercadoLibreCategorySuggestion(
    val categoryId: String,
    val categoryName: String,
    val pathFromRoot: List<String> = emptyList(),
)

/**
 * A single attribute definition for a category, from `GET /categories/{id}/attributes`. Required
 * attributes vary widely by category (LEDGER D62: from 1 to 7 in real sampled categories) — callers
 * must render a form per-category, never assume a fixed set of fields.
 */
@Serializable
data class MercadoLibreCategoryAttribute(
    val id: String,
    val name: String,
    val valueType: String,
    val required: Boolean,
    val catalogRequired: Boolean,
    val values: List<String> = emptyList(),
)

/** Item-level rules for a category, from `GET /categories/{id}` (`.settings`). */
@Serializable
data class MercadoLibreCategorySettings(
    val maxTitleLength: Int? = null,
    val maxPicturesPerItem: Int? = null,
    val minimumPrice: Double? = null,
    val minimumPriceCurrency: String? = null,
    val itemConditions: List<String> = emptyList(),
    val catalogDomain: String? = null,
)
