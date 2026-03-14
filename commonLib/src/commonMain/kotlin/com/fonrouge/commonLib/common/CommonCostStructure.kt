package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.CostStructure
import com.fonrouge.commonLib.model.CostStructureFilter
import com.fonrouge.commonLib.model.CostStructureId

data object CommonCostStructure : ICommonContainer<CostStructure, CostStructureId, CostStructureFilter>(
    itemKClass = CostStructure::class,
    idSerializer = OId.serializer(CostStructure.serializer()),
    apiFilterSerializer = CostStructureFilter.serializer(),
    labelItem = "Cost Structure",
    labelList = "Cost Structures"
)
