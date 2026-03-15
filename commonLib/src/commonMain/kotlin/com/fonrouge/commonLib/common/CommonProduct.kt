package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId

data object CommonProduct : ICommonContainer<Product, ProductId, ProductFilter>(
    itemKClass = Product::class,
    filterKClass = ProductFilter::class,
    labelItem = "Product",
    labelList = "Products"
)
