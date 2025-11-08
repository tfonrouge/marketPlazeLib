package com.fonrouge.commonLib.model

import kotlinx.serialization.Serializable

/**
 * Represents a unique identifier for a product in a specific marketplace.
 *
 * This data class is designed to associate a product identifier of a generic type
 * with a marketplace. The marketplace is represented by the [MarketPlaceId] enum,
 * ensuring a standardized representation of supported marketplaces.
 *
 * @param PID The type of the product ID, providing flexibility for various formats.
 * @property marketPlaceId The marketplace where the product is listed, identified by [MarketPlaceId].
 * @property productID The unique identifier of the product within the specified marketplace.
 */
@Serializable
data class MarketProductId<PID : Any>(
    val marketPlaceId: MarketPlaceId,
    val productID: PID
)
