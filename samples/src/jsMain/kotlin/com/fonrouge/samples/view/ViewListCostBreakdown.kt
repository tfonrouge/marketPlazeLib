package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCostBreakdown
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.view.colDef.colDefBreakdownNetProfit
import com.fonrouge.commonLib.view.colDef.colDefBreakdownProfitMargin
import com.fonrouge.commonLib.view.colDef.colDefBreakdownSalePrice
import com.fonrouge.commonLib.view.colDef.colDefBreakdownTotalCosts
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewListCostBreakdown
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition

open class ViewListCostBreakdown :
    ViewList<CommonCostBreakdown, CostBreakdown, CostBreakdownId, CostBreakdownFilter, MarketplaceId>(
        configView = ConfigViewListCostBreakdown,
    ) {
    override fun columnDefinitionList(): List<ColumnDefinition<CostBreakdown>> = listOf(
        colDefBreakdownSalePrice(),
        colDefBreakdownTotalCosts(),
        colDefBreakdownNetProfit(),
        colDefBreakdownProfitMargin(),
    )

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListCostBreakdown)
    }
}
