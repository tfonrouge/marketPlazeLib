package com.fonrouge.mercadoLibreLib.common

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
abstract class CommonMercadoLibreProduct<FILT : IApiFilter<*>>(
    apiFilterSerializer: KSerializer<FILT>,
) : ICommonContainer<MercadoLibreProduct, StringId<MercadoLibreProduct>, FILT>(
    itemKClass = MercadoLibreProduct::class,
    idSerializer = StringId.serializer(serializer()),
    apiFilterSerializer = apiFilterSerializer,
    labelItem = "MercadoLibre Product",
    labelList = "MercadoLibre Products"
)
