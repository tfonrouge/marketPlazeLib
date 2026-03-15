package com.fonrouge.samples.services

import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.*
import com.fonrouge.samples.collections.SampleCollections
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import io.ktor.server.application.*

class SampleItemService(
    val call: ApplicationCall,
    private val collections: SampleCollections,
) : ISampleItemService {

    override suspend fun marketplace(iApiItem: IApiItem<Marketplace, MarketplaceId, MarketplaceFilter>): ItemState<Marketplace> =
        collections.MarketplaceColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun product(iApiItem: IApiItem<Product, ProductId, ProductFilter>): ItemState<Product> =
        collections.ProductColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun marketplaceListing(iApiItem: IApiItem<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter>): ItemState<MarketplaceListing> =
        collections.MarketplaceListingColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun costStructure(iApiItem: IApiItem<CostStructure, CostStructureId, CostStructureFilter>): ItemState<CostStructure> =
        collections.CostStructureColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun costBreakdown(iApiItem: IApiItem<CostBreakdown, CostBreakdownId, CostBreakdownFilter>): ItemState<CostBreakdown> =
        collections.CostBreakdownColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun competitorPrice(iApiItem: IApiItem<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>): ItemState<CompetitorPrice> =
        collections.CompetitorPriceColl.apiItemProcess(iApiItem = iApiItem, call = null)

    override suspend fun shopifyProduct(iApiItem: IApiItem<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter>): ItemState<ShopifyProduct> =
        collections.ShopifyProductColl.apiItemProcess(iApiItem = iApiItem, call = null)
}
