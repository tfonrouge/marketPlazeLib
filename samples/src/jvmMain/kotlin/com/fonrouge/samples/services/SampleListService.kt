package com.fonrouge.samples.services

import com.fonrouge.base.api.ApiList
import com.fonrouge.base.state.ListState
import com.fonrouge.commonLib.model.*
import com.fonrouge.samples.collections.SampleCollections
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import io.ktor.server.application.*

/**
 * Sample list service implementation.
 * Each method delegates to the collection's apiListProcess().
 *
 * In a real app, this is registered via:
 *   registerService<ISampleListService> { SampleListService(it) }
 */
class SampleListService(val call: ApplicationCall) : ISampleListService {

    companion object {
        lateinit var collections: SampleCollections
    }

    override suspend fun marketplace(apiList: ApiList<MarketplaceFilter>): ListState<Marketplace> =
        collections.MarketplaceColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun product(apiList: ApiList<ProductFilter>): ListState<Product> =
        collections.ProductColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun marketplaceListing(apiList: ApiList<MarketplaceListingFilter>): ListState<MarketplaceListing> =
        collections.MarketplaceListingColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun costStructure(apiList: ApiList<CostStructureFilter>): ListState<CostStructure> =
        collections.CostStructureColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun costBreakdown(apiList: ApiList<CostBreakdownFilter>): ListState<CostBreakdown> =
        collections.CostBreakdownColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun competitorPrice(apiList: ApiList<CompetitorPriceFilter>): ListState<CompetitorPrice> =
        collections.CompetitorPriceColl.apiListProcess(call = call, apiList = apiList)

    override suspend fun shopifyProduct(apiList: ApiList<ShopifyProductFilter>): ListState<ShopifyProduct> =
        collections.ShopifyProductColl.apiListProcess(call = call, apiList = apiList)
}
