package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.text.text

fun ViewFormPanel<CompetitorPrice>.competitorPriceFormFields() {
    formRow {
        formColumn(12) {
            text(label = "Competitor Name:").bind(CompetitorPrice::competitorName, required = true)
        }
    }
}
