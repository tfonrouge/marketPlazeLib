package com.fonrouge.shopifyLib.model.product

import com.fonrouge.base.types.OId
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import com.fonrouge.shopifyLib.model.IStore
import kotlinx.datetime.Instant

interface IProductBase {
    val storeId: OId<IStore>
    val productType: String
    val publishedAt: Instant?
    val title: String
    val vendor: String
}
