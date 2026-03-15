package com.fonrouge.amazonLib.colls

import com.fonrouge.commonLib.colls.MarketPlazeCollections

abstract class IAmazonCollections<UID : Any> : MarketPlazeCollections() {
    abstract val AmazonProductColl: IAmazonProductColl<*>
}
