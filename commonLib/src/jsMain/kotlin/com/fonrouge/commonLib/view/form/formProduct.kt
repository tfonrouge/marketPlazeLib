package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Product
import com.fonrouge.fullStack.layout.formColumn
import com.fonrouge.fullStack.layout.formRow
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.text.text

fun ViewFormPanel<Product>.productFormFields() {
    formRow {
        formColumn(3) {
            text(label = "SKU:").bind(Product::sku, required = true)
        }
        formColumn(9) {
            text(label = "Name:").bind(Product::name, required = true)
        }
    }
}
