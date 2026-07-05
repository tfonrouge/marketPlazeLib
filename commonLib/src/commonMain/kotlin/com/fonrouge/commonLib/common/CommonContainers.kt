package com.fonrouge.commonLib.common

import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.*

data object CommonMarketplace : ICommonContainer<Marketplace, MarketplaceId, MarketplaceFilter>(
    itemKClass = Marketplace::class,
    filterKClass = MarketplaceFilter::class,
    labelItem = "Marketplace",
    labelList = "Marketplaces"
)

data object CommonProduct : ICommonContainer<Product, ProductId, ProductFilter>(
    itemKClass = Product::class,
    filterKClass = ProductFilter::class,
    labelItem = "Product",
    labelList = "Products"
)

data object CommonMarketplaceListing : ICommonContainer<MarketplaceListing, MarketplaceListingId, MarketplaceListingFilter>(
    itemKClass = MarketplaceListing::class,
    filterKClass = MarketplaceListingFilter::class,
    labelItem = "Marketplace Listing",
    labelList = "Marketplace Listings"
)

data object CommonMarketplaceListingDraft : ICommonContainer<MarketplaceListingDraft, MarketplaceListingDraftId, MarketplaceListingDraftFilter>(
    itemKClass = MarketplaceListingDraft::class,
    filterKClass = MarketplaceListingDraftFilter::class,
    labelItem = "Marketplace Listing Draft",
    labelList = "Marketplace Listing Drafts"
)

data object CommonCostStructure : ICommonContainer<CostStructure, CostStructureId, CostStructureFilter>(
    itemKClass = CostStructure::class,
    filterKClass = CostStructureFilter::class,
    labelItem = "Cost Structure",
    labelList = "Cost Structures"
)

data object CommonCostBreakdown : ICommonContainer<CostBreakdown, CostBreakdownId, CostBreakdownFilter>(
    itemKClass = CostBreakdown::class,
    filterKClass = CostBreakdownFilter::class,
    labelItem = "Cost Breakdown",
    labelList = "Cost Breakdowns"
)

data object CommonCompetitorPrice : ICommonContainer<CompetitorPrice, CompetitorPriceId, CompetitorPriceFilter>(
    itemKClass = CompetitorPrice::class,
    filterKClass = CompetitorPriceFilter::class,
    labelItem = "Competitor Price",
    labelList = "Competitor Prices"
)
