package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonMarketplace
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.view.form.marketplaceFormFields
import com.fonrouge.fullStack.config.configViewItem
import com.fonrouge.fullStack.view.ViewItem
import io.kvision.form.FormPanel
import com.fonrouge.samples.services.ISampleItemService
import io.kvision.core.Container

class ViewItemMarketplace : ViewItem<Marketplace, MarketplaceId, MarketplaceFilter>(
    configView = configViewItem,
) {
    companion object {
        val configViewItem = configViewItem(
            viewKClass = ViewItemMarketplace::class,
            commonContainer = CommonMarketplace,
            apiItemFun = ISampleItemService::marketplace,
        )
    }

    override fun Container.pageItemBody(): FormPanel<Marketplace> = viewFormPanel {
        marketplaceFormFields()
    }
}
