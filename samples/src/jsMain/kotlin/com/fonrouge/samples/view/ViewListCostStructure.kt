package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCostStructure
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.view.colDef.colDefCostStructureCommissionCount
import com.fonrouge.commonLib.view.colDef.colDefCostStructureFeeCount
import com.fonrouge.commonLib.view.colDef.colDefCostStructureName
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.services.ISampleListService
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition

class ViewListCostStructure :
    ViewList<CostStructure, CostStructureId, CostStructureFilter, MarketplaceId>(
        configView = configViewList,
    ) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListCostStructure::class,
            commonContainer = CommonCostStructure,
            apiListFun = ISampleListService::costStructure,
        )
    }

    override fun columnDefinitionList(): List<ColumnDefinition<CostStructure>> = listOf(
        colDefCostStructureName(),
        colDefCostStructureCommissionCount(),
        colDefCostStructureFeeCount(),
    )

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListCostStructure)
    }
}
