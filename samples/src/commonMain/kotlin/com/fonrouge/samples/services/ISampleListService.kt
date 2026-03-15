package com.fonrouge.samples.services

import com.fonrouge.base.api.ApiList
import com.fonrouge.base.state.ListState
import com.fonrouge.commonLib.model.*
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import dev.kilua.rpc.annotations.RpcBindingRoute
import dev.kilua.rpc.annotations.RpcService

@RpcService
interface ISampleListService {
    @RpcBindingRoute("ISampleListService.marketplace")
    suspend fun marketplace(apiList: ApiList<MarketplaceFilter>): ListState<Marketplace>

    @RpcBindingRoute("ISampleListService.product")
    suspend fun product(apiList: ApiList<ProductFilter>): ListState<Product>

    @RpcBindingRoute("ISampleListService.marketplaceListing")
    suspend fun marketplaceListing(apiList: ApiList<MarketplaceListingFilter>): ListState<MarketplaceListing>

    @RpcBindingRoute("ISampleListService.costStructure")
    suspend fun costStructure(apiList: ApiList<CostStructureFilter>): ListState<CostStructure>

    @RpcBindingRoute("ISampleListService.costBreakdown")
    suspend fun costBreakdown(apiList: ApiList<CostBreakdownFilter>): ListState<CostBreakdown>

    @RpcBindingRoute("ISampleListService.competitorPrice")
    suspend fun competitorPrice(apiList: ApiList<CompetitorPriceFilter>): ListState<CompetitorPrice>

    @RpcBindingRoute("ISampleListService.shopifyProduct")
    suspend fun shopifyProduct(apiList: ApiList<ShopifyProductFilter>): ListState<ShopifyProduct>
}
