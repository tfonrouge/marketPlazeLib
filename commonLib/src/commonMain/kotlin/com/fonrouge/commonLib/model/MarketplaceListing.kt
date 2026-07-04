package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias MarketplaceListingId = OId<MarketplaceListing>

@Serializable
data class MarketplaceListing(
    override val _id: MarketplaceListingId = OId(),
    val productId: ProductId? = null,
    val marketplaceId: MarketplaceId,
    val externalId: String,
    val title: String,
    /**
     * Raw ERP item id (e.g. a SAE Clave/SKU) for consumers that link a listing directly to
     * their own inventory entity instead of going through the generic [Product] catalog.
     * Plain String (not a typed id) so this library stays ERP-agnostic — see mppErsaPack
     * MarketPlace(MODULE) LEDGER D57 for why `productId` (OId<Product>, real Mongo ObjectId
     * on the JVM) cannot itself hold an arbitrary ERP id string.
     */
    val erpItemId: String? = null,
    val price: Money = Money.ZERO,
    val publishEnabled: Boolean = false,
    val discount: Double = 0.0,
    val listingUrl: String? = null,
    val presentationIndex: Int? = null,
    val publicationType: PublicationType = PublicationType.CLASICA,
    val cargoFijoOverride: Money? = null,
    val envioOverride: Money? = null,
    val descriptionHtml: String? = null,
    val barcode: String? = null,
    val vendor: String? = null,
    val weight: Double? = null,
    val tags: List<String> = emptyList(),
    val inventoryQuantity: Int = 0,
    val productType: String? = null,
) : BaseDoc<MarketplaceListingId>
