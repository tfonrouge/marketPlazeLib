package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.CostBreakdown
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import kotlin.reflect.KProperty1

fun <T : Any> colDefBreakdownSalePrice(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Sale Price",
    field = fieldName(*(kPath + CostBreakdown::salePrice)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        Span(content = "$${cents / 100.0}")
    }
)

fun <T : Any> colDefBreakdownNetProfit(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Net Profit",
    field = fieldName(*(kPath + CostBreakdown::netProfit)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        val className = if (cents >= 0) "text-success fw-bold" else "text-danger fw-bold"
        Span(content = "$${cents / 100.0}", className = className)
    }
)

fun <T : Any> colDefBreakdownProfitMargin(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Margin %",
    field = fieldName(*(kPath + CostBreakdown::profitMarginPercent)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val pct = cell.getValue() as? Double ?: 0.0
        val className = if (pct >= 0) "text-success" else "text-danger"
        val rounded = (pct * 10).toLong() / 10.0
        Span(content = "$rounded%", className = className)
    }
)

fun <T : Any> colDefBreakdownTotalCosts(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Total Costs",
    field = fieldName(*(kPath + CostBreakdown::totalCosts)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        Span(content = "$${cents / 100.0}")
    }
)
