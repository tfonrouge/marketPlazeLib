package com.fonrouge.samples.services

import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.*
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import dev.kilua.rpc.annotations.RpcBindingRoute
import dev.kilua.rpc.annotations.RpcService

@RpcService
interface ISampleItemService {
    @RpcBindingRoute("ISampleItemService.marketplace")
    suspend fun marketplace(iApiItem: IApiItem<Marketplace, MarketplaceId, MarketplaceFilter>): ItemState<Marketplace>

    @RpcBindingRoute("ISampleItemService.product")
    suspend fun product(iApiItem: IApiItem<Product, ProductId, ProductFilter>): ItemState<Product>

    @RpcBindingRoute("ISampleItemService.marketplaceListing")
    suspend fun marketplaceListing(iApiItem: IApiItem<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter>): ItemState<MarketplaceListing>

    @RpcBindingRoute("ISampleItemService.costStructure")
    suspend fun costStructure(iApiItem: IApiItem<CostStructure, CostStructureId, CostStructureFilter>): ItemState<CostStructure>

    @RpcBindingRoute("ISampleItemService.costBreakdown")
    suspend fun costBreakdown(iApiItem: IApiItem<CostBreakdown, CostBreakdownId, CostBreakdownFilter>): ItemState<CostBreakdown>

    @RpcBindingRoute("ISampleItemService.competitorPrice")
    suspend fun competitorPrice(iApiItem: IApiItem<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>): ItemState<CompetitorPrice>

    @RpcBindingRoute("ISampleItemService.shopifyProduct")
    suspend fun shopifyProduct(iApiItem: IApiItem<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter>): ItemState<ShopifyProduct>
}
