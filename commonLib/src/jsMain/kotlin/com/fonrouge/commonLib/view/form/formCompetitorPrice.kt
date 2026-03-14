package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.text.text

fun ViewFormPanel<CompetitorPrice>.competitorPriceFormFields() {
    text(label = "Competitor Name:").bind(CompetitorPrice::competitorName, required = true)
}
