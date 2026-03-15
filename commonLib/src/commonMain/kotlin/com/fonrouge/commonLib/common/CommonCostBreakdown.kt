package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.CostBreakdown
import com.fonrouge.commonLib.model.CostBreakdownFilter
import com.fonrouge.commonLib.model.CostBreakdownId

data object CommonCostBreakdown : ICommonContainer<CostBreakdown, CostBreakdownId, CostBreakdownFilter>(
    itemKClass = CostBreakdown::class,
    filterKClass = CostBreakdownFilter::class,
    labelItem = "Cost Breakdown",
    labelList = "Cost Breakdowns"
)
