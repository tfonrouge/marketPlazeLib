package com.fonrouge.shopifyLib.colls

abstract class IShopifyCollections<UID : Any>() {
    abstract val ShopifyBusinessEntityColl: IShopifyBusinessEntityColl<*>
    abstract val ShopifyProductColl: IShopifyProductColl<*, *, *>
    abstract val StoreColl: IStoreColl<*, *, *, *>
}
