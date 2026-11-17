package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayFilter

data object CommonMercadoLibreAdsDay : ICommonContainer<MercadoLibreAdsDay, StringId<MercadoLibreAdsDay>, MercadoLibreAdsDayFilter>(
    itemKClass = MercadoLibreAdsDay::class,
    filterKClass = MercadoLibreAdsDayFilter::class,
    labelItem = "Mercado Ads: día",
    labelList = "Mercado Ads: días"
)
