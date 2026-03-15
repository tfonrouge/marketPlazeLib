package com.fonrouge.samples.view

import com.fonrouge.base.fieldName
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.fullStack.tabulator.fsTabulator
import com.fonrouge.fullStack.view.ViewList
import com.fonrouge.samples.services.ISampleListService
import com.fonrouge.shopifyLib.common.CommonShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import io.kvision.core.Container
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor

class ViewListShopifyProduct :
    ViewList<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter, MarketplaceId>(
        configView = configViewList,
    ) {
    companion object {
        val configViewList = configViewList(
            viewKClass = ViewListShopifyProduct::class,
            commonContainer = CommonShopifyProduct,
            apiListFun = ISampleListService::shopifyProduct,
        )
    }

    override fun columnDefinitionList(): List<ColumnDefinition<ShopifyProduct>> = listOf(
        ColumnDefinition(title = "Handle", field = fieldName(ShopifyProduct::handle), headerFilter = Editor.INPUT, headerSort = true),
        ColumnDefinition(title = "Title", field = fieldName(ShopifyProduct::title), headerFilter = Editor.INPUT, headerSort = true),
        ColumnDefinition(title = "Vendor", field = fieldName(ShopifyProduct::vendor), headerFilter = Editor.INPUT, headerSort = true),
        ColumnDefinition(title = "Type", field = fieldName(ShopifyProduct::productType), headerSort = true),
        ColumnDefinition(title = "Inventory", field = fieldName(ShopifyProduct::totalInventory), headerSort = true),
    )

    override fun Container.pageListBody() {
        fsTabulator(this@ViewListShopifyProduct)
    }
}
