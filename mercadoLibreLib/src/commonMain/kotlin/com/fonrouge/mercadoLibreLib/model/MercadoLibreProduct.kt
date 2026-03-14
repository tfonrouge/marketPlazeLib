package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class MercadoLibreProduct(
    override val _id: StringId<MercadoLibreProduct>,
    val marketplaceId: MarketplaceId,
    val title: String,
    val categoryId: String = "",
    val price: Money = Money.ZERO,
    val currencyId: String = "MXN",
    val availableQuantity: Int = 0,
    val soldQuantity: Int = 0,
    val condition: String = "new",
    val permalink: String = "",
    val thumbnail: String = "",
    val pictures: List<Picture> = emptyList(),
    val status: String = "active",
) : BaseDoc<StringId<MercadoLibreProduct>> {
    @Serializable
    data class Picture(
        val id: String,
        val url: String,
        val secureUrl: String? = null,
        val size: String? = null,
    )
}
