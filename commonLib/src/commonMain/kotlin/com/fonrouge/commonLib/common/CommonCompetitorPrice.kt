package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.CompetitorPriceId

data object CommonCompetitorPrice : ICommonContainer<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>(
    itemKClass = CompetitorPrice::class,
    idSerializer = OId.serializer(CompetitorPrice.serializer()),
    apiFilterSerializer = CompetitorPriceFilter.serializer(),
    labelItem = "Competitor Price",
    labelList = "Competitor Prices"
)
