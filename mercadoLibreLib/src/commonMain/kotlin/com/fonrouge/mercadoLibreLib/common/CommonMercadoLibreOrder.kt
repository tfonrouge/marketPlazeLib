package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreOrder
import com.fonrouge.mercadoLibreLib.model.MercadoLibreOrderFilter

data object CommonMercadoLibreOrder : ICommonContainer<MercadoLibreOrder, StringId<MercadoLibreOrder>, MercadoLibreOrderFilter>(
    itemKClass = MercadoLibreOrder::class,
    filterKClass = MercadoLibreOrderFilter::class,
    labelItem = "MercadoLibre Order",
    labelList = "MercadoLibre Orders"
)