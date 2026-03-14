package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonMarketplace
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class MarketplaceColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CommonMarketplace, Marketplace, MarketplaceId, MarketplaceFilter, UID>(
    commonContainer = CommonMarketplace,
    debug = debug,
)
