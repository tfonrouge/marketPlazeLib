package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Product
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.text.text

fun ViewFormPanel<Product>.productFormFields() {
    text(label = "SKU:").bind(Product::sku, required = true)
    text(label = "Name:").bind(Product::name, required = true)
}
