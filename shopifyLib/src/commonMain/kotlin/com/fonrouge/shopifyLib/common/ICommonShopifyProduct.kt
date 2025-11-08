package com.fonrouge.shopifyLib.common

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.model.ShopifyProduct
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
abstract class ICommonShopifyProduct<FILT : IApiFilter<*>>(
    apiFilterSerializer: KSerializer<FILT>,
) : ICommonContainer<ShopifyProduct, StringId<ShopifyProduct>, FILT>(
    itemKClass = ShopifyProduct::class,
    idSerializer = StringId.serializer(serializer()),
    apiFilterSerializer = apiFilterSerializer,
    labelItem = "Shopify Product",
    labelList = "Shopify Products"
)
