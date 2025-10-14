package com.fonrouge.shopifyLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.serializers.FSOffsetDateTimeSerializer
import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import io.kvision.types.OffsetDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ShopifyProduct(
    override val _id: StringId<ShopifyProduct>,
    @ShopifyField(ignore = true) val storeId: OId<IStore> = OId(),
    val handle: String,
    val productType: String,
    @Serializable(with = FSOffsetDateTimeSerializer::class) val publishedAt: OffsetDateTime?,
    val title: String,
    val vendor: String,
) : BaseDoc<StringId<ShopifyProduct>>