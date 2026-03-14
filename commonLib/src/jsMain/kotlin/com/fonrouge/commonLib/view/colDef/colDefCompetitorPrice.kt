package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.CompetitorPrice
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor
import kotlin.reflect.KProperty1

fun <T : Any> colDefCompetitorName(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Competitor",
    field = fieldName(*(kPath + CompetitorPrice::competitorName)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefCompetitorPriceAmount(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Price",
    field = fieldName(*(kPath + CompetitorPrice::price)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        Span(content = "$${cents / 100.0}")
    }
)

fun <T : Any> colDefCompetitorObservedAt(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Observed At",
    field = fieldName(*(kPath + CompetitorPrice::observedAt)),
    headerSort = true,
    headerSortTristate = true,
)
