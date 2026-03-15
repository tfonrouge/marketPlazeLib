package com.fonrouge.samples.view

import com.fonrouge.commonLib.common.CommonProduct
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId
import com.fonrouge.commonLib.view.colDef.colDefProductBaseCost
import com.fonrouge.commonLib.view.colDef.colDefProductName
import com.fonrouge.commonLib.view.colDef.colDefProductSku
import com.fonrouge.commonLib.view.colDef.colDefProductVariantCount
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.services.ISampleListService
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition

class ViewListProduct : ViewList<Product, ProductId, ProductFilter, Unit>(
    configView = configViewList,
) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListProduct::class,
            commonContainer = CommonProduct,
            apiListFun = ISampleListService::product,
        )
    }

    override fun columnDefinitionList(): List<ColumnDefinition<Product>> = listOf(
        colDefProductSku(),
        colDefProductName(),
        colDefProductBaseCost(),
        colDefProductVariantCount(),
    )

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListProduct)
    }
}
