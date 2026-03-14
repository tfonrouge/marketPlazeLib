package com.fonrouge.samples.configView

import com.fonrouge.commonLib.common.*
import com.fonrouge.commonLib.model.*
import com.fonrouge.fullStack.config.configViewItem
import com.fonrouge.fullStack.config.configViewList
import com.fonrouge.samples.services.ISampleItemService
import com.fonrouge.samples.services.ISampleListService
import com.fonrouge.samples.view.*
import com.fonrouge.shopifyLib.common.CommonShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import com.fonrouge.base.types.StringId

/**
 * Central ConfigView registry — binds ViewList/ViewItem classes to
 * RPC service functions and CommonContainers.
 *
 * The consuming app must set:
 *   ConfigViewItem.serviceManager = SampleItemServiceManager
 *   ConfigViewList.serviceManager = SampleListServiceManager
 */
object SampleConfigView {

    // --- Marketplace ---
    val ConfigViewListMarketplace = configViewList(
        viewKClass = ViewListMarketplace::class,
        commonContainer = CommonMarketplace,
        apiListFun = ISampleListService::marketplace,
    )

    val ConfigViewItemMarketplace = configViewItem(
        viewKClass = ViewItemMarketplace::class,
        commonContainer = CommonMarketplace,
        apiItemFun = ISampleItemService::marketplace,
    )

    // --- Product ---
    val ConfigViewListProduct = configViewList(
        viewKClass = ViewListProduct::class,
        commonContainer = CommonProduct,
        apiListFun = ISampleListService::product,
    )

    val ConfigViewItemProduct = configViewItem(
        viewKClass = ViewItemProduct::class,
        commonContainer = CommonProduct,
        apiItemFun = ISampleItemService::product,
    )

    // --- Marketplace Listing ---
    val ConfigViewListMarketplaceListing = configViewList(
        viewKClass = ViewListMarketplaceListing::class,
        commonContainer = CommonMarketplaceListing,
        apiListFun = ISampleListService::marketplaceListing,
    )

    // --- Cost Structure ---
    val ConfigViewListCostStructure = configViewList(
        viewKClass = ViewListCostStructure::class,
        commonContainer = CommonCostStructure,
        apiListFun = ISampleListService::costStructure,
    )

    // --- Cost Breakdown ---
    val ConfigViewListCostBreakdown = configViewList(
        viewKClass = ViewListCostBreakdown::class,
        commonContainer = CommonCostBreakdown,
        apiListFun = ISampleListService::costBreakdown,
    )

    // --- Competitor Price ---
    val ConfigViewListCompetitorPrice = configViewList(
        viewKClass = ViewListCompetitorPrice::class,
        commonContainer = CommonCompetitorPrice,
        apiListFun = ISampleListService::competitorPrice,
    )

    val ConfigViewItemCompetitorPrice = configViewItem(
        viewKClass = ViewItemCompetitorPrice::class,
        commonContainer = CommonCompetitorPrice,
        apiItemFun = ISampleItemService::competitorPrice,
    )

    // --- Shopify Product ---
    val ConfigViewListShopifyProduct = configViewList(
        viewKClass = ViewListShopifyProduct::class,
        commonContainer = CommonShopifyProduct,
        apiListFun = ISampleListService::shopifyProduct,
    )
}
