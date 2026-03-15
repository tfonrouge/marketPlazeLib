package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProductFilter

data object CommonMercadoLibreProduct : ICommonContainer<MercadoLibreProduct, StringId<MercadoLibreProduct>, MercadoLibreProductFilter>(
    itemKClass = MercadoLibreProduct::class,
    filterKClass = MercadoLibreProductFilter::class,
    labelItem = "MercadoLibre Product",
    labelList = "MercadoLibre Products"
)
