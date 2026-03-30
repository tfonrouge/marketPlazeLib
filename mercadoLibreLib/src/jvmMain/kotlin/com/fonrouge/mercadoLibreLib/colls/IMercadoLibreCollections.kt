package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.commonLib.colls.MarketPlazeCollections

abstract class IMercadoLibreCollections<UID : Any> : MarketPlazeCollections() {
    abstract val MercadoLibreProductColl: IMercadoLibreProductColl<*>
    abstract val MercadoLibreOrderColl: IMercadoLibreOrderColl<*>
}
