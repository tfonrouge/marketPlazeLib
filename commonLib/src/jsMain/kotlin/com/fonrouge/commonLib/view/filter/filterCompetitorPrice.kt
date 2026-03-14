package com.fonrouge.commonLib.view.filter

import com.fonrouge.commonLib.model.CompetitorPriceFilter
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.fullStack.view.ViewList
import io.kvision.core.Container
import io.kvision.core.onChange
import io.kvision.form.text.text
import io.kvision.offcanvas.offcanvas
import io.kvision.panel.fieldsetPanel

fun ViewList<*, *, *, CompetitorPriceFilter, MarketplaceId>.offCanvasCompetitorPriceFilter(container: Container) =
    container.offcanvas(caption = "Filter", scrollableBody = true) {
        fieldsetPanel(legend = "Competitor Price Filter") {
            text(label = "Competitor Name:", value = apiFilter.competitorName) {
                onChange {
                    apiFilter = apiFilter.copy(competitorName = value?.ifBlank { null })
                }
            }
        }
    }
