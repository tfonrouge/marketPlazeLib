package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.CompetitorPriceId

data object CommonCompetitorPrice : ICommonContainer<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>(
    itemKClass = CompetitorPrice::class,
    filterKClass = CompetitorPriceFilter::class,
    labelItem = "Competitor Price",
    labelList = "Competitor Prices"
)
