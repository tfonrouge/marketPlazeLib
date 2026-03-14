package com.fonrouge.samples.services

import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.state.ItemState
import com.fonrouge.commonLib.model.*
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import com.fonrouge.base.types.StringId
import dev.kilua.rpc.annotations.RpcService

@RpcService
interface ISampleItemService {
    suspend fun marketplace(iApiItem: IApiItem<Marketplace, MarketplaceId, MarketplaceFilter>): ItemState<Marketplace>
    suspend fun product(iApiItem: IApiItem<Product, ProductId, ProductFilter>): ItemState<Product>
    suspend fun marketplaceListing(iApiItem: IApiItem<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter>): ItemState<MarketplaceListing>
    suspend fun costStructure(iApiItem: IApiItem<CostStructure, CostStructureId, CostStructureFilter>): ItemState<CostStructure>
    suspend fun costBreakdown(iApiItem: IApiItem<CostBreakdown, CostBreakdownId, CostBreakdownFilter>): ItemState<CostBreakdown>
    suspend fun competitorPrice(iApiItem: IApiItem<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>): ItemState<CompetitorPrice>
    suspend fun shopifyProduct(iApiItem: IApiItem<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter>): ItemState<ShopifyProduct>
}
