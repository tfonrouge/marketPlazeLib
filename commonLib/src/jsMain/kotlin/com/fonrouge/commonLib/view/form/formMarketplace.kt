package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import io.kvision.form.FormPanel
import io.kvision.form.check.checkBox
import io.kvision.form.text.text

fun FormPanel<Marketplace>.marketplaceFormFields() {
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
    formRow {
        formColumn(3) {
            checkBox(label = "Default").bind(Marketplace::isDefault)
        }
        formColumn(3) {
            checkBox(label = "IVA Included").bind(Marketplace::ivaIncluded)
        }
    }
}
