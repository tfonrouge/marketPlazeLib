package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCostStructure
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.view.colDef.colDefCostStructureCommissionCount
import com.fonrouge.commonLib.view.colDef.colDefCostStructureFeeCount
import com.fonrouge.commonLib.view.colDef.colDefCostStructureName
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewListCostStructure
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition

open class ViewListCostStructure :
    ViewList<CommonCostStructure, CostStructure, CostStructureId, CostStructureFilter, MarketplaceId>(
        configView = ConfigViewListCostStructure,
    ) {
    override fun columnDefinitionList(): List<ColumnDefinition<CostStructure>> = listOf(
        colDefCostStructureName(),
        colDefCostStructureCommissionCount(),
        colDefCostStructureFeeCount(),
    )

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListCostStructure)
    }
}
