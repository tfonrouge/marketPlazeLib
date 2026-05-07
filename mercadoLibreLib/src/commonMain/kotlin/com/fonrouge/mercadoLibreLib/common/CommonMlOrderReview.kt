package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.api.ApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MlOrderReview

data object CommonMlOrderReview : ICommonContainer<MlOrderReview, StringId<MlOrderReview>, ApiFilter>(
    itemKClass = MlOrderReview::class,
    filterKClass = ApiFilter::class,
    labelItem = "ML Order Review",
    labelList = "ML Order Reviews",
)