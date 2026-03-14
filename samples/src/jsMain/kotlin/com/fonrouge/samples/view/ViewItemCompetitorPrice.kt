package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonCompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.CompetitorPriceId
import com.fonrouge.commonLib.view.form.competitorPriceFormFields
import com.fonrouge.fullStack.view.ViewFormPanel
import com.fonrouge.fullStack.view.ViewItem
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewItemCompetitorPrice
import io.kvision.core.Container

class ViewItemCompetitorPrice : ViewItem<CommonCompetitorPrice, CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>(
    configView = ConfigViewItemCompetitorPrice,
) {
    override fun Container.pageItemBody(): ViewFormPanel<CompetitorPrice> = viewFormPanel {
        competitorPriceFormFields()
    }
}
