package com.fonrouge.shopifyLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter

data object CommonShopifyProduct : ICommonContainer<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter>(
    itemKClass = ShopifyProduct::class,
    filterKClass = ShopifyProductFilter::class,
    labelItem = "Shopify Product",
    labelList = "Shopify Products"
)
