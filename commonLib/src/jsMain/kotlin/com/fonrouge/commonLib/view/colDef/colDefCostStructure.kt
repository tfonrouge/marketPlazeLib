package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.CostStructure
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor
import kotlin.reflect.KProperty1

fun <T : Any> colDefCostStructureName(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Name",
    field = fieldName(*(kPath + CostStructure::name)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefCostStructureCommissionCount(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Commissions",
    field = fieldName(*(kPath + CostStructure::commissions)),
    formatterComponentFunction = { cell, _, _ ->
        val list = cell.getValue()
        val count = (list as? List<*>)?.size ?: 0
        Span(content = "$count")
    }
)

fun <T : Any> colDefCostStructureFeeCount(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Fees",
    field = fieldName(*(kPath + CostStructure::fees)),
    formatterComponentFunction = { cell, _, _ ->
        val list = cell.getValue()
        val count = (list as? List<*>)?.size ?: 0
        Span(content = "$count")
    }
)
