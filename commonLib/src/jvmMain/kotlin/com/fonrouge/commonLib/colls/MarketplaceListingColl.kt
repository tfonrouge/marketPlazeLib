package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonMarketplaceListing
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.MarketplaceListingFilter
import com.fonrouge.commonLib.model.MarketplaceListingId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class MarketplaceListingColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CommonMarketplaceListing, MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter, UID>(
    commonContainer = CommonMarketplaceListing,
    debug = debug,
)
