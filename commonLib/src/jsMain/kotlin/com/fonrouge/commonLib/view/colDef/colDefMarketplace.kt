package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.Marketplace
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor
import kotlin.reflect.KProperty1

fun <T : Any> colDefMarketplaceName(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Name",
    field = fieldName(*(kPath + Marketplace::name)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefMarketplaceType(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Type",
    field = fieldName(*(kPath + Marketplace::type)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefMarketplaceEnabled(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Enabled",
    field = fieldName(*(kPath + Marketplace::enabled)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val enabled = cell.getValue() as? Boolean ?: false
        Span(
            content = if (enabled) "Active" else "Inactive",
            className = if (enabled) "badge bg-success" else "badge bg-secondary"
        )
    }
)
