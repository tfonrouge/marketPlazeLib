package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.CostStructure
import com.fonrouge.commonLib.model.CostStructureFilter
import com.fonrouge.commonLib.model.CostStructureId

data object CommonCostStructure : ICommonContainer<CostStructure, CostStructureId, CostStructureFilter>(
    itemKClass = CostStructure::class,
    filterKClass = CostStructureFilter::class,
    labelItem = "Cost Structure",
    labelList = "Cost Structures"
)
