package com.fonrouge.samples.services

import com.fonrouge.base.api.ApiList
import com.fonrouge.base.state.ListState
import com.fonrouge.commonLib.model.*
import com.fonrouge.samples.collections.SampleCollections
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import io.ktor.server.application.*

class SampleListService(
    val call: ApplicationCall,
    private val collections: SampleCollections,
) : ISampleListService {

    override suspend fun marketplace(apiList: ApiList<MarketplaceFilter>): ListState<Marketplace> =
        collections.MarketplaceColl.apiListProcess(apiList = apiList)

    override suspend fun product(apiList: ApiList<ProductFilter>): ListState<Product> =
        collections.ProductColl.apiListProcess(apiList = apiList)

    override suspend fun marketplaceListing(apiList: ApiList<MarketplaceListingFilter>): ListState<MarketplaceListing> =
        collections.MarketplaceListingColl.apiListProcess(apiList = apiList)

    override suspend fun costStructure(apiList: ApiList<CostStructureFilter>): ListState<CostStructure> =
        collections.CostStructureColl.apiListProcess(apiList = apiList)

    override suspend fun costBreakdown(apiList: ApiList<CostBreakdownFilter>): ListState<CostBreakdown> =
        collections.CostBreakdownColl.apiListProcess(apiList = apiList)

    override suspend fun competitorPrice(apiList: ApiList<CompetitorPriceFilter>): ListState<CompetitorPrice> =
        collections.CompetitorPriceColl.apiListProcess(apiList = apiList)

    override suspend fun shopifyProduct(apiList: ApiList<ShopifyProductFilter>): ListState<ShopifyProduct> =
        collections.ShopifyProductColl.apiListProcess(apiList = apiList)
}
