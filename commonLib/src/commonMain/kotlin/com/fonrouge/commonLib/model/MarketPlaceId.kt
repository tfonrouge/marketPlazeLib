package com.fonrouge.commonLib.model

import com.fonrouge.base.enums.XEnum
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a marketplace type where products can be listed or sold.
 *
 * This enum provides a standardized way to identify marketplaces using a
 * specific encoded value and is designed to work with serialization mechanisms.
 * Each marketplace is represented with a short identifier (e.g., "S" for Shopify).
 *
 * @property encoded The serialized value that uniquely identifies each marketplace.
 */
@Serializable
enum class MarketPlaceId(override val encoded: String) : XEnum {
    @SerialName("S")
    Shopify("S"),

    @SerialName("ML")
    MercadoLibre("ML"),

    @SerialName("A")
    Amazon("A"),

    @SerialName("CS")
    ClaroShop("CS"),
}
