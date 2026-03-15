package com.fonrouge.shopifyLib.common

import com.fonrouge.base.api.ApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.model.ShopifyBusinessEntity

data object CommonShopifyBusinessEntity :
    ICommonContainer<ShopifyBusinessEntity, StringId<ShopifyBusinessEntity>, ApiFilter>(
        itemKClass = ShopifyBusinessEntity::class,
        filterKClass = ApiFilter::class
    )
