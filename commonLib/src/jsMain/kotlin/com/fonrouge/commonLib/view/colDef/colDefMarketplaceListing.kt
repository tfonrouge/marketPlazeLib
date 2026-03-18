package com.fonrouge.commonLib.view.colDef

import com.fonrouge.base.fieldName
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.model.PublicationType
import io.kvision.html.Span
import io.kvision.tabulator.ColumnDefinition
import io.kvision.tabulator.Editor
import kotlin.reflect.KProperty1

fun <T : Any> colDefListingExternalId(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "External ID",
    field = fieldName(*(kPath + MarketplaceListing::externalId)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefListingTitle(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Title",
    field = fieldName(*(kPath + MarketplaceListing::title)),
    headerFilter = Editor.INPUT,
    headerSort = true,
    headerSortTristate = true,
)

fun <T : Any> colDefListingPrice(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Price",
    field = fieldName(*(kPath + MarketplaceListing::price)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val cents = cell.getValue() as? Long ?: 0L
        Span(content = "$${cents / 100.0}")
    }
)

fun <T : Any> colDefListingPublishEnabled(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Published",
    field = fieldName(*(kPath + MarketplaceListing::publishEnabled)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val enabled = cell.getValue() as? Boolean ?: false
        Span(
            content = if (enabled) "Yes" else "No",
            className = if (enabled) "badge bg-success" else "badge bg-secondary"
        )
    }
)

fun <T : Any> colDefListingPublicationType(vararg kPath: KProperty1<*, *>) = ColumnDefinition<T>(
    title = "Pub. Type",
    field = fieldName(*(kPath + MarketplaceListing::publicationType)),
    headerSort = true,
    headerSortTristate = true,
    formatterComponentFunction = { cell, _, _ ->
        val value = cell.getValue()?.toString() ?: PublicationType.CLASICA.name
        val label = if (value == PublicationType.PREMIUM.name) "Premium" else "Clásica"
        val cls = if (value == PublicationType.PREMIUM.name) "badge bg-warning" else "badge bg-info"
        Span(content = label, className = cls)
    }
)
