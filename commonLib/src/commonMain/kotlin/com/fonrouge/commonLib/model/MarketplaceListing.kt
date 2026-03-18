package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias MarketplaceListingId = OId<MarketplaceListing>

@Serializable
data class MarketplaceListing(
    override val _id: MarketplaceListingId = OId(),
    val productId: ProductId,
    val marketplaceId: MarketplaceId,
    val externalId: String,
    val title: String,
    val price: Money = Money.ZERO,
    val publishEnabled: Boolean = false,
    val discount: Double = 0.0,
    val listingUrl: String? = null,
    val presentationIndex: Int? = null,
    val publicationType: PublicationType = PublicationType.CLASICA,
    val cargoFijoOverride: Money? = null,
    val envioOverride: Money? = null,
) : BaseDoc<MarketplaceListingId>
