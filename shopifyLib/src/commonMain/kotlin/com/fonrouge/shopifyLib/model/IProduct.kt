package com.fonrouge.shopifyLib.model

import com.apollographql.apollo.annotations.ApolloAdaptableWith
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.type.Product
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@ApolloAdaptableWith(Product::class)
abstract class IProduct : BaseDoc<StringId<IProduct>> {
    abstract val storeId: OId<IStore>
    abstract val handle: String
    abstract val productType: String
    abstract val publishedAt: Instant?
    abstract val title: String
    abstract val vendor: String
}
