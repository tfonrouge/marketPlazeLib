package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonMarketplace
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.view.colDef.colDefMarketplaceEnabled
import com.fonrouge.commonLib.view.colDef.colDefMarketplaceName
import com.fonrouge.commonLib.view.colDef.colDefMarketplaceType
import com.fonrouge.commonLib.view.filter.bannerLegendMarketplaceFilter
import com.fonrouge.commonLib.view.filter.offCanvasMarketplaceFilter
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewListMarketplace
import io.kvision.core.Container
import io.kvision.offcanvas.Offcanvas
import io.kvision.tabulator.ColumnDefinition

open class ViewListMarketplace : ViewList<CommonMarketplace, Marketplace, MarketplaceId, MarketplaceFilter, Unit>(
    configView = ConfigViewListMarketplace,
) {
    override fun columnDefinitionList(): List<ColumnDefinition<Marketplace>> = listOf(
        colDefMarketplaceName(),
        colDefMarketplaceType(),
        colDefMarketplaceEnabled(),
    )

    override fun Container.bannerLegend() = bannerLegendMarketplaceFilter(this)
    override fun Container.buildOffCanvasFilterView(): Offcanvas = offCanvasMarketplaceFilter(this)

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListMarketplace)
    }
}
