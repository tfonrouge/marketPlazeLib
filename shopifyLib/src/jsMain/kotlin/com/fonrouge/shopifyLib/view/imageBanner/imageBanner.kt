package com.fonrouge.shopifyLib.view.imageBanner

import io.kvision.core.*
import io.kvision.html.image
import io.kvision.panel.hPanel
import io.kvision.utils.px
import io.kvision.utils.rem

fun Container.imageBanner() {
    hPanel(spacing = 5, className = "container-fluid") {
        addBsBgColor(BsBgColor.LIGHT)
        for (i in 1..5) image(src = "images/casa_dulce_logo1.png", responsive = true) {
            border = Border(width = 1.px, style = BorderStyle.SOLID, color = Color("gray"))
            height = 10.rem
        }
    }
}
