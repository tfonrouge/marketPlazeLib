package com.fonrouge.shopifyLib.model.product

import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.model.IStore
import kotlinx.datetime.Instant

interface IProductBase {
    val storeId: OId<IStore>
    val handle: String
    val productType: String
    val publishedAt: Instant?
    val title: String
    val vendor: String
}
