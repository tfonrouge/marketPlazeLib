package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.commonLib.model.MarketplaceId

data object CommonMarketplace : ICommonContainer<Marketplace, MarketplaceId, MarketplaceFilter>(
    itemKClass = Marketplace::class,
    idSerializer = OId.serializer(Marketplace.serializer()),
    apiFilterSerializer = MarketplaceFilter.serializer(),
    labelItem = "Marketplace",
    labelList = "Marketplaces"
)
