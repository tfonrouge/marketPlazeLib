package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonProduct
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId
import com.fonrouge.commonLib.view.form.productFormFields
import com.fonrouge.fullStack.view.ViewFormPanel
import com.fonrouge.fullStack.view.ViewItem
import com.fonrouge.samples.configView.SampleConfigView.ConfigViewItemProduct
import io.kvision.core.Container

class ViewItemProduct : ViewItem<CommonProduct, Product, ProductId, ProductFilter>(
    configView = ConfigViewItemProduct,
) {
    override fun Container.pageItemBody(): ViewFormPanel<Product> = viewFormPanel {
        productFormFields()
    }
}
