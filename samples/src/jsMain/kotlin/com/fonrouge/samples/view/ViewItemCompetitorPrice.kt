package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.CompetitorPriceId
import com.fonrouge.commonLib.view.form.competitorPriceFormFields
import com.fonrouge.fullStack.config.configViewItem
import com.fonrouge.fullStack.view.ViewFormPanel
import com.fonrouge.fullStack.view.ViewItem
import com.fonrouge.samples.services.ISampleItemService
import io.kvision.core.Container

class ViewItemCompetitorPrice : ViewItem<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>(
    configView = configViewItem,
) {
    companion object {
        val configViewItem = configViewItem(
            viewKClass = ViewItemCompetitorPrice::class,
            commonContainer = CommonCompetitorPrice,
            apiItemFun = ISampleItemService::competitorPrice,
        )
    }

    override fun Container.pageItemBody(): ViewFormPanel<CompetitorPrice> = viewFormPanel {
        competitorPriceFormFields()
    }
}
