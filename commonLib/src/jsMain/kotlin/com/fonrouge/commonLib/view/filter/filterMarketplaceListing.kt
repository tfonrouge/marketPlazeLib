package com.fonrouge.commonLib.view.filter

import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListingFilter
import com.fonrouge.fullStack.view.ViewList
import io.kvision.core.Container
import io.kvision.core.onChange
import io.kvision.form.check.radioGroup
import io.kvision.offcanvas.offcanvas
import io.kvision.panel.fieldsetPanel

fun ViewList<*, *, *, MarketplaceListingFilter, MarketplaceId>.offCanvasListingFilter(container: Container) =
    container.offcanvas(caption = "Filter", scrollableBody = true) {
        fieldsetPanel(legend = "Listing Filter") {
            radioGroup(
                label = "Published:",
                options = listOf("" to "All", "true" to "Published", "false" to "Unpublished"),
                value = apiFilter.publishEnabled?.toString() ?: "",
            ) {
                onChange {
                    apiFilter = apiFilter.copy(
                        publishEnabled = when (value) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                    )
                }
            }
        }
    }
