package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonCompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.CompetitorPriceId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class CompetitorPriceColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CommonCompetitorPrice, CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter, UID>(
    commonContainer = CommonCompetitorPrice,
    debug = debug,
)
