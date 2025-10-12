package com.fonrouge.shopifyLib.model.product

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import com.fonrouge.shopifyLib.model.IStore
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ShopifyProduct(
    override val _id: StringId<ShopifyProduct>,
    override val handle: String,
    @ShopifyField(ignore = true)
    override val storeId: OId<IStore> = OId(),
    override val productType: String,
    override val publishedAt: Instant?,
    override val title: String,
    override val vendor: String,
) : BaseDoc<StringId<ShopifyProduct>>, IProductBase
