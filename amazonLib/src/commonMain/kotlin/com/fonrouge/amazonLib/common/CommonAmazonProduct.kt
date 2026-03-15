package com.fonrouge.amazonLib.common

import com.fonrouge.amazonLib.model.AmazonProduct
import com.fonrouge.amazonLib.model.AmazonProductFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId

data object CommonAmazonProduct : ICommonContainer<AmazonProduct, StringId<AmazonProduct>, AmazonProductFilter>(
    itemKClass = AmazonProduct::class,
    filterKClass = AmazonProductFilter::class,
    labelItem = "Amazon Product",
    labelList = "Amazon Products"
)
