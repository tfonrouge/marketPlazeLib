package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.CompetitorPrice
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import io.kvision.form.FormPanel
import io.kvision.form.text.text

fun FormPanel<CompetitorPrice>.competitorPriceFormFields() {
    formRow {
        formColumn(12) {
            text(label = "Competitor Name:").bind(CompetitorPrice::competitorName, required = true)
        }
    }
}
