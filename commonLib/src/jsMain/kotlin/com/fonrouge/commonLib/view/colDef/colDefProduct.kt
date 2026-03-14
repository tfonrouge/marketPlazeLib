package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.Product
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor
import kotlin.reflect.KProperty1

fun <T : Any> colDefProductSku(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "SKU",
    field = fieldName(*(kPath + Product::sku)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefProductName(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Name",
    field = fieldName(*(kPath + Product::name)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefProductBaseCost(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Base Cost",
    field = fieldName(*(kPath + Product::baseCost)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        Span(content = "$${cents / 100.0}")
    }
)

fun <T : Any> colDefProductVariantCount(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Variants",
    field = fieldName(*(kPath + Product::variants)),
    formatterComponentFunction = { cell, _, _ ->
        val list = cell.getValue()
        val count = (list as? List<*>)?.size ?: 0
        Span(content = "$count")
    }
)
