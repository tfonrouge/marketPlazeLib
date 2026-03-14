package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonMarketplace
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.view.form.marketplaceFormFields
import com.fonrouge.fullStack.view.ViewFormPanel
import com.fonrouge.fullStack.view.ViewItem
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewItemMarketplace
import io.kvision.core.Container

class ViewItemMarketplace : ViewItem<CommonMarketplace, Marketplace, MarketplaceId, MarketplaceFilter>(
    configView = ConfigViewItemMarketplace,
) {
    override fun Container.pageItemBody(): ViewFormPanel<Marketplace> = viewFormPanel {
        marketplaceFormFields()
    }
}
