package com.fonrouge.shopifyLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.model.ShopifyCollection
import com.fonrouge.shopifyLib.model.ShopifyCollectionFilter

data object CommonShopifyCollection : ICommonContainer<ShopifyCollection, StringId<ShopifyCollection>, ShopifyCollectionFilter>(
    itemKClass = ShopifyCollection::class,
    filterKClass = ShopifyCollectionFilter::class,
    labelItem = "Shopify Collection",
    labelList = "Shopify Collections"
)
