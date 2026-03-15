package com.fonrouge.samples.collections

import com.fonrouge.base.api.ApiFilter
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.common.*
import com.fonrouge.commonLib.model.*
import com.fonrouge.fullStack.memoryDb.InMemoryRepository
import com.fonrouge.shopifyLib.common.CommonShopifyProduct
import com.fonrouge.shopifyLib.common.CommonShopifyBusinessEntity
import com.fonrouge.shopifyLib.model.ShopifyBusinessEntity
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter

typealias SampleUserId = String

class SampleCollections {
    val MarketplaceColl = InMemoryRepository<Marketplace, MarketplaceId, MarketplaceFilter, SampleUserId>(
        commonContainer = CommonMarketplace,
    )
    val ProductColl = InMemoryRepository<Product, ProductId, ProductFilter, SampleUserId>(
        commonContainer = CommonProduct,
    )
    val MarketplaceListingColl = InMemoryRepository<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter, SampleUserId>(
        commonContainer = CommonMarketplaceListing,
    )
    val CostStructureColl = InMemoryRepository<CostStructure, CostStructureId, CostStructureFilter, SampleUserId>(
        commonContainer = CommonCostStructure,
    )
    val CostBreakdownColl = InMemoryRepository<CostBreakdown, CostBreakdownId, CostBreakdownFilter, SampleUserId>(
        commonContainer = CommonCostBreakdown,
    )
    val CompetitorPriceColl = InMemoryRepository<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter, SampleUserId>(
        commonContainer = CommonCompetitorPrice,
    )
    val ShopifyProductColl = InMemoryRepository<ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter, SampleUserId>(
        commonContainer = CommonShopifyProduct,
    )
    val ShopifyBusinessEntityColl = InMemoryRepository<ShopifyBusinessEntity, StringId<ShopifyBusinessEntity>, ApiFilter, SampleUserId>(
        commonContainer = CommonShopifyBusinessEntity,
    )
}
