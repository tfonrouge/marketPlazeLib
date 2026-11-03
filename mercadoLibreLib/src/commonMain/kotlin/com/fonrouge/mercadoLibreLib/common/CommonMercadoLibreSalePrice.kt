package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceFilter
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceObservation

data object CommonMercadoLibreSalePrice : ICommonContainer<MercadoLibreSalePriceObservation, StringId<MercadoLibreSalePriceObservation>, MercadoLibreSalePriceFilter>(
    itemKClass = MercadoLibreSalePriceObservation::class,
    filterKClass = MercadoLibreSalePriceFilter::class,
    labelItem = "MercadoLibre Sale Price",
    labelList = "MercadoLibre Sale Prices"
)
