package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Product
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import io.kvision.form.FormPanel
import io.kvision.form.text.text

fun FormPanel<Product>.productFormFields() {
    formRow {
        formColumn(3) {
            text(label = "SKU:").bind(Product::sku, required = true)
        }
        formColumn(9) {
            text(label = "Name:").bind(Product::name, required = true)
        }
    }
}
