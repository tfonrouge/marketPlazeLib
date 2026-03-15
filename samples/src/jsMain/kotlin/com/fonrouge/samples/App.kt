package com.fonrouge.samples

import com.fonrouge.fullStack.config.ViewRegistry
import com.fonrouge.fullStack.view.KVWebManager
import com.fonrouge.fullStack.view.KVWebManager.viewStateObservableValue
import com.fonrouge.fullStack.view.showView
import com.fonrouge.samples.services.ISampleItemService
import com.fonrouge.samples.services.ISampleListService
import com.fonrouge.samples.view.*
import dev.kilua.rpc.getServiceManager
import io.kvision.*
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.remote.registerRemoteTypes
import io.kvision.state.bind

class App : Application() {

    override fun start(state: Map<String, Any>) {
        val listServiceManager = getServiceManager<ISampleListService>()
        val itemServiceManager = getServiceManager<ISampleItemService>()
        ViewRegistry.listServiceManager = listServiceManager
        ViewRegistry.itemServiceManager = itemServiceManager

        // Force companion object initialization to register ConfigViews
        ViewListMarketplace.configViewList
        ViewItemMarketplace.configViewItem
        ViewListProduct.configViewList
        ViewItemProduct.configViewItem
        ViewListMarketplaceListing.configViewList
        ViewListCostStructure.configViewList
        ViewListCostBreakdown.configViewList
        ViewListCompetitorPrice.configViewList
        ViewItemCompetitorPrice.configViewItem
        ViewListShopifyProduct.configViewList

        root("kvapp") {
            vPanel {
                bind(viewStateObservableValue) { viewState ->
                    viewState?.let { showView(it) }
                }
            }
        }

        KVWebManager.initialize {
            frontEndAppName = "MarketPlazeLib Sample"
            frontEndVersion = "2.0.0"
            defaultView = ViewListMarketplace.configViewList
        }
    }
}

fun main() {
    registerRemoteTypes()
    startApplication(
        ::App,
        js("import.meta.webpackHot").unsafeCast<Hot?>(),
        BootstrapModule,
        BootstrapCssModule,
        ToastifyModule,
        FontAwesomeModule,
        TabulatorModule,
        TabulatorCssBootstrapModule,
        CoreModule,
    )
}
