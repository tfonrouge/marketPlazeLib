package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.check.checkBox
import io.kvision.form.text.Text
import io.kvision.form.text.TextArea
import io.kvision.form.text.text

fun ViewFormPanel<Marketplace>.marketplaceFormFields() {
    formRow {
        formColumn(6) {
            text(label = "Name:").bind(Marketplace::name, required = true)
        }
        formColumn(3) {
            text(label = "Type:").bind(Marketplace::type, required = true)
        }
        formColumn(3) {
            checkBox(label = "Enabled").bind(Marketplace::enabled)
        }
    }
}
