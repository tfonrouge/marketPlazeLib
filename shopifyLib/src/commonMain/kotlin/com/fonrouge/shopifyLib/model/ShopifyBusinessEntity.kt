package com.fonrouge.shopifyLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import kotlinx.serialization.Serializable

@Serializable
data class ShopifyBusinessEntity(
    override val _id: StringId<ShopifyBusinessEntity>,
    val archived: Boolean,
    val companyName: String?,
    val displayName: String,
    val primary: Boolean,
) : BaseDoc<StringId<ShopifyBusinessEntity>>
