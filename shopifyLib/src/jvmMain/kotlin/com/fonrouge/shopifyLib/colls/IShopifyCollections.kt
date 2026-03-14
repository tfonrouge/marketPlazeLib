package com.fonrouge.shopifyLib.colls

import com.fonrouge.commonLib.colls.MarketPlazeCollections

abstract class IShopifyCollections : MarketPlazeCollections() {
    abstract val ShopifyBusinessEntityColl: IShopifyBusinessEntityColl<*>
    abstract val ShopifyProductColl: IShopifyProductColl<*>
}
