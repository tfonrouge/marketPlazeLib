package com.fonrouge.commonLib.colls

abstract class MarketPlazeCollections {
    abstract val MarketplaceColl: MarketplaceColl<*>
    abstract val ProductColl: ProductColl<*>
    abstract val MarketplaceListingColl: MarketplaceListingColl<*>
    abstract val MarketplaceListingDraftColl: MarketplaceListingDraftColl<*>
    abstract val CostStructureColl: CostStructureColl<*>
    abstract val CostBreakdownColl: CostBreakdownColl<*>
    abstract val CompetitorPriceColl: CompetitorPriceColl<*>
}
