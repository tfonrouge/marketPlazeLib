package com.fonrouge.mercadoLibreLib.model

import kotlinx.serialization.Serializable

/**
 * Body for `POST /items` — fields confirmed by the read-only spike (mppErsaPack MarketPlace(MODULE)
 * LEDGER D62): price/stock required, category-specific [attributes], `listing_type_id` tiers.
 * [description] and the exact behavior of [pictureUrls] (sent as `pictures: [{source: url}]`, letting
 * ML re-host) are NOT yet verified against a real write — see LEDGER D68/D62 gaps.
 */
@Serializable
data class MercadoLibreItemPayload(
    val title: String,
    val categoryId: String,
    val price: Double,
    val currencyId: String,
    val availableQuantity: Int,
    val condition: String,
    val listingTypeId: String,
    val buyingMode: String = "buy_it_now",
    val attributes: Map<String, String> = emptyMap(),
    val pictureUrls: List<String> = emptyList(),
    val description: String? = null,
)

/**
 * Result of `POST /items`. Unlike the `SimpleState`-shaped results of [IMercadoLibrePushService]
 * (which only ever updates an existing item), creation needs to hand back the new item id on
 * success — callers persist it as `MarketplaceListing.externalId` /
 * `MarketplaceListingDraft.publishedListingId`.
 */
@Serializable
data class MercadoLibreCreateItemResult(
    val isOk: Boolean,
    val itemId: String? = null,
    val permalink: String? = null,
    val msgError: String? = null,
)
