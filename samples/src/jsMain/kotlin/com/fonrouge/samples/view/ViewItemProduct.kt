package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonProduct
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId
import com.fonrouge.commonLib.view.form.productFormFields
import com.fonrouge.fullStack.config.configViewItem
import com.fonrouge.fullStack.view.ViewItem
import io.kvision.form.FormPanel
import com.fonrouge.samples.services.ISampleItemService
import io.kvision.core.Container

class ViewItemProduct : ViewItem<Product, ProductId, ProductFilter>(
    configView = configViewItem,
) {
    companion object {
        val configViewItem = configViewItem(
            viewKClass = ViewItemProduct::class,
            commonContainer = CommonProduct,
            apiItemFun = ISampleItemService::product,
        )
    }

    override fun Container.pageItemBody(): FormPanel<Product> = viewFormPanel {
        productFormFields()
    }
}
