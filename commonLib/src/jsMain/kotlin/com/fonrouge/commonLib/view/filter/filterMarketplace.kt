package com.fonrouge.commonLib.view.filter

import com.fonrouge.commonLib.model.MarketplaceFilter
import com.fonrouge.fullStack.view.ViewList
import io.kvision.core.Container
import io.kvision.core.onChange
import io.kvision.form.check.radioGroup
import io.kvision.form.text.text
import io.kvision.html.Span
import io.kvision.offcanvas.offcanvas
import io.kvision.panel.fieldsetPanel
import io.kvision.panel.vPanel

fun ViewList<*, *, MarketplaceFilter, *>.offCanvasMarketplaceFilter(container: Container) = run {
    val viewList = this
    container.offcanvas(caption = "Filter", scrollableBody = true) {
        fieldsetPanel(legend = "Marketplace Filter") {
            text(label = "Type:", value = viewList.apiFilter.type) {
                onChange {
                    viewList.apiFilter = viewList.apiFilter.copy(type = value?.ifBlank { null })
                }
            }
            radioGroup(
                label = "Status:",
                options = listOf("" to "All", "true" to "Enabled", "false" to "Disabled"),
                value = viewList.apiFilter.enabled?.toString() ?: "",
            ) {
                onChange {
                    viewList.apiFilter = viewList.apiFilter.copy(
                        enabled = when (value) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                    )
                }
            }
        }
    }
}

fun ViewList<*, *, MarketplaceFilter, *>.bannerLegendMarketplaceFilter(container: Container) {
    val viewList = this
    container.vPanel {
        viewList.apiFilter.type?.let {
            add(Span(content = "<b>Type:</b> $it", rich = true))
        }
        viewList.apiFilter.enabled?.let {
            add(Span(content = "<b>Status:</b> ${if (it) "Enabled" else "Disabled"}", rich = true))
        }
    }
}
