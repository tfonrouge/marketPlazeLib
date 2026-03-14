package com.fonrouge.amazonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class AmazonProduct(
    override val _id: StringId<AmazonProduct>,
    val marketplaceId: MarketplaceId,
    val asin: String,
    val title: String,
    val brand: String = "",
    val category: String = "",
    val price: Money = Money.ZERO,
    val currencyCode: String = "USD",
    val availability: String = "in_stock",
    val fulfillmentChannel: String = "DEFAULT",
    val images: List<AmazonImage> = emptyList(),
    val bulletPoints: List<String> = emptyList(),
) : BaseDoc<StringId<AmazonProduct>> {
    @Serializable
    data class AmazonImage(
        val url: String,
        val variant: String = "MAIN",
        val width: Int? = null,
        val height: Int? = null,
    )
}
