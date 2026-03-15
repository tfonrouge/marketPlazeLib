package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.MarketplaceListingFilter
import com.fonrouge.commonLib.model.MarketplaceListingId

data object CommonMarketplaceListing : ICommonContainer<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter>(
    itemKClass = MarketplaceListing::class,
    filterKClass = MarketplaceListingFilter::class,
    labelItem = "Marketplace Listing",
    labelList = "Marketplace Listings"
)
