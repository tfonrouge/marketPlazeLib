package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayFilter
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsPeriod

data object CommonMercadoLibreAdsPeriod : ICommonContainer<MercadoLibreAdsPeriod, StringId<MercadoLibreAdsPeriod>, MercadoLibreAdsDayFilter>(
    itemKClass = MercadoLibreAdsPeriod::class,
    filterKClass = MercadoLibreAdsDayFilter::class,
    labelItem = "Mercado Ads: periodo",
    labelList = "Mercado Ads: periodos"
)
