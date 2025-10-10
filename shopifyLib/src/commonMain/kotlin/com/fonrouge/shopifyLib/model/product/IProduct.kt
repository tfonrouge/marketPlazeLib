package com.fonrouge.shopifyLib.model.product

import com.apollographql.apollo.annotations.ApolloAdaptableWith
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.type.Product
import kotlinx.serialization.Serializable

@Serializable
@ApolloAdaptableWith(Product::class)
abstract class IProduct : BaseDoc<StringId<IProduct>>, IProductBase
