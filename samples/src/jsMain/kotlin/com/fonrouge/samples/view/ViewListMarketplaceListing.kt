package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonMarketplaceListing
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.MarketplaceListingFilter
import com.fonrouge.commonLib.model.MarketplaceListingId
import com.fonrouge.commonLib.view.colDef.colDefListingExternalId
import com.fonrouge.commonLib.view.colDef.colDefListingPrice
import com.fonrouge.commonLib.view.colDef.colDefListingPublishEnabled
import com.fonrouge.commonLib.view.colDef.colDefListingTitle
import com.fonrouge.commonLib.view.filter.offCanvasListingFilter
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.services.ISampleListService
import io.kvision.core.Container
import io.kvision.offcanvas.Offcanvas
import io.kvision.tabulator.ColumnDefinition

class ViewListMarketplaceListing :
    ViewList<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter, MarketplaceId>(
        configView = configViewList,
    ) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListMarketplaceListing::class,
            commonContainer = CommonMarketplaceListing,
            apiListFun = ISampleListService::marketplaceListing,
        )
    }

    override fun columnDefinitionList(): List<ColumnDefinition<MarketplaceListing>> = listOf(
        colDefListingExternalId(),
        colDefListingTitle(),
        colDefListingPrice(),
        colDefListingPublishEnabled(),
    )

    override fun Container.buildOffCanvasFilterView(): Offcanvas = offCanvasListingFilter(this)

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListMarketplaceListing)
    }
}
