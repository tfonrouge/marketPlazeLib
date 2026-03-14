package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId

data object CommonProduct : ICommonContainer<Product, ProductId, ProductFilter>(
    itemKClass = Product::class,
    idSerializer = OId.serializer(Product.serializer()),
    apiFilterSerializer = ProductFilter.serializer(),
    labelItem = "Product",
    labelList = "Products"
)
