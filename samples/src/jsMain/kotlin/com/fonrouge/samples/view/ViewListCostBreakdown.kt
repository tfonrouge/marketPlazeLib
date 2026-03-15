package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCostBreakdown
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.view.colDef.colDefBreakdownNetProfit
import com.fonrouge.commonLib.view.colDef.colDefBreakdownProfitMargin
import com.fonrouge.commonLib.view.colDef.colDefBreakdownSalePrice
import com.fonrouge.commonLib.view.colDef.colDefBreakdownTotalCosts
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.services.ISampleListService
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition

class ViewListCostBreakdown :
    ViewList<CostBreakdown, CostBreakdownId, CostBreakdownFilter, MarketplaceId>(
        configView = configViewList,
    ) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListCostBreakdown::class,
            commonContainer = CommonCostBreakdown,
            apiListFun = ISampleListService::costBreakdown,
        )
    }

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
