package com.fonrouge.commonLib.view.form

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.fullStack.view.ViewFormPanel
import io.kvision.form.check.checkBox
import io.kvision.form.text.text

fun ViewFormPanel<Marketplace>.marketplaceFormFields() {
    text(label = "Name:").bind(Marketplace::name, required = true)
    text(label = "Type:").bind(Marketplace::type, required = true)
    checkBox(label = "Enabled").bind(Marketplace::enabled)
}
