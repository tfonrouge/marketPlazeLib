package com.fonrouge.samples.services

import com.fonrouge.base.api.ApiList
import com.fonrouge.base.state.ListState
import com.fonrouge.commonLib.model.*
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import dev.kilua.rpc.annotations.RpcService

@RpcService
interface ISampleListService {
    suspend fun marketplace(apiList: ApiList<MarketplaceFilter>): ListState<Marketplace>
    suspend fun product(apiList: ApiList<ProductFilter>): ListState<Product>
    suspend fun marketplaceListing(apiList: ApiList<MarketplaceListingFilter>): ListState<MarketplaceListing>
    suspend fun costStructure(apiList: ApiList<CostStructureFilter>): ListState<CostStructure>
    suspend fun costBreakdown(apiList: ApiList<CostBreakdownFilter>): ListState<CostBreakdown>
    suspend fun competitorPrice(apiList: ApiList<CompetitorPriceFilter>): ListState<CompetitorPrice>
    suspend fun shopifyProduct(apiList: ApiList<ShopifyProductFilter>): ListState<ShopifyProduct>
}
