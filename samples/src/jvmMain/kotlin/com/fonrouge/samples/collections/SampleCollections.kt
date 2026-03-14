package com.fonrouge.samples.collections

import com.fonrouge.commonLib.colls.*
import com.fonrouge.shopifyLib.colls.IShopifyBusinessEntityColl
import com.fonrouge.shopifyLib.colls.IShopifyCollections
import com.fonrouge.shopifyLib.colls.IShopifyProductColl

/**
 * Sample concrete collections. In a real app, each collection class
 * implements `userCollFun` for RBAC and optionally overrides
 * `lookupFun`, `matchStage`, `afterLookupSortStage` for custom
 * MongoDB aggregation pipelines.
 *
 * Usage:
 *   val collections = SampleCollections()
 *   val marketplaces = collections.MarketplaceColl.findList(...)
 */

typealias SampleUserId = String

class SampleMarketplaceColl : MarketplaceColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleProductColl : ProductColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleMarketplaceListingColl : MarketplaceListingColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleCostStructureColl : CostStructureColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleCostBreakdownColl : CostBreakdownColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleCompetitorPriceColl : CompetitorPriceColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleShopifyProductColl : IShopifyProductColl<SampleUserId>() {
    override val userCollFun = { null }
}

class SampleShopifyBusinessEntityColl : IShopifyBusinessEntityColl<SampleUserId>() {
    override val userCollFun = { null }
}

/**
 * Concrete collection factory wiring all collections together.
 * Extends IShopifyCollections which itself extends MarketPlazeCollections,
 * so it provides both base marketplace and Shopify-specific collections.
 */
class SampleCollections : IShopifyCollections() {
    override val MarketplaceColl = SampleMarketplaceColl()
    override val ProductColl = SampleProductColl()
    override val MarketplaceListingColl = SampleMarketplaceListingColl()
    override val CostStructureColl = SampleCostStructureColl()
    override val CostBreakdownColl = SampleCostBreakdownColl()
    override val CompetitorPriceColl = SampleCompetitorPriceColl()
    override val ShopifyProductColl = SampleShopifyProductColl()
    override val ShopifyBusinessEntityColl = SampleShopifyBusinessEntityColl()
}
