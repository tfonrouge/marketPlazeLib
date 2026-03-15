package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonCostBreakdown
import com.fonrouge.commonLib.model.CostBreakdown
import com.fonrouge.commonLib.model.CostBreakdownFilter
import com.fonrouge.commonLib.model.CostBreakdownId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class CostBreakdownColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CostBreakdown, CostBreakdownId, CostBreakdownFilter, UID>(
    commonContainer = CommonCostBreakdown,
    debug = debug,
)
