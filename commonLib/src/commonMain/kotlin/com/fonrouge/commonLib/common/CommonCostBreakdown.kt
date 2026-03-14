package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.CostBreakdown
import com.fonrouge.commonLib.model.CostBreakdownFilter
import com.fonrouge.commonLib.model.CostBreakdownId

data object CommonCostBreakdown : ICommonContainer<CostBreakdown, CostBreakdownId, CostBreakdownFilter>(
    itemKClass = CostBreakdown::class,
    idSerializer = OId.serializer(CostBreakdown.serializer()),
    apiFilterSerializer = CostBreakdownFilter.serializer(),
    labelItem = "Cost Breakdown",
    labelList = "Cost Breakdowns"
)
