package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonCostStructure
import com.fonrouge.commonLib.model.CostStructure
import com.fonrouge.commonLib.model.CostStructureFilter
import com.fonrouge.commonLib.model.CostStructureId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class CostStructureColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CommonCostStructure, CostStructure, CostStructureId, CostStructureFilter, UID>(
    commonContainer = CommonCostStructure,
    debug = debug,
)
