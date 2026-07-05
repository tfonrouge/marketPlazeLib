package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonMarketplaceListingDraft
import com.fonrouge.commonLib.model.MarketplaceListingDraft
import com.fonrouge.commonLib.model.MarketplaceListingDraftFilter
import com.fonrouge.commonLib.model.MarketplaceListingDraftId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class MarketplaceListingDraftColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MarketplaceListingDraft, MarketplaceListingDraftId, MarketplaceListingDraftFilter, UID>(
    commonContainer = CommonMarketplaceListingDraft,
    debug = debug,
)
