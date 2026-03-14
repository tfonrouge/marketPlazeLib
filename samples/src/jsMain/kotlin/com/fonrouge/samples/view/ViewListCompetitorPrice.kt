package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCompetitorPrice
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.view.colDef.colDefCompetitorName
import com.fonrouge.commonLib.view.colDef.colDefCompetitorObservedAt
import com.fonrouge.commonLib.view.colDef.colDefCompetitorPriceAmount
import com.fonrouge.commonLib.view.filter.offCanvasCompetitorPriceFilter
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewListCompetitorPrice
import io.kvision.core.Container
import io.kvision.offcanvas.Offcanvas
import io.kvision.tabulator.ColumnDefinition

open class ViewListCompetitorPrice :
    ViewList<CommonCompetitorPrice, CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter, MarketplaceId>(
        configView = ConfigViewListCompetitorPrice,
    ) {
    override fun columnDefinitionList(): List<ColumnDefinition<CompetitorPrice>> = listOf(
        colDefCompetitorName(),
        colDefCompetitorPriceAmount(),
        colDefCompetitorObservedAt(),
    )

    override fun Container.buildOffCanvasFilterView(): Offcanvas = offCanvasCompetitorPriceFilter(this)

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListCompetitorPrice)
    }
}
