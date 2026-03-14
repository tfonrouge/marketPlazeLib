package com.fonrouge.amazonLib.common

import com.fonrouge.amazonLib.model.AmazonProduct
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
abstract class CommonAmazonProduct<FILT : IApiFilter<*>>(
    apiFilterSerializer: KSerializer<FILT>,
) : ICommonContainer<AmazonProduct, StringId<AmazonProduct>, FILT>(
    itemKClass = AmazonProduct::class,
    idSerializer = StringId.serializer(serializer()),
    apiFilterSerializer = apiFilterSerializer,
    labelItem = "Amazon Product",
    labelList = "Amazon Products"
)
