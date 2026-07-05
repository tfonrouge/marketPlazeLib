package com.fonrouge.commonLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias MarketplaceListingDraftId = OId<MarketplaceListingDraft>

/**
 * A new article being prepared for publication to a marketplace — the host app's own catalog
 * (ERP item, product, etc.) is the source; this is the staging record until the marketplace
 * confirms creation, at which point it graduates into a real [MarketplaceListing] (see
 * [publishedListingId]). Same shape of pattern as an app's own "prospect gradúa a entidad real"
 * lifecycle (mppErsaPack MarketPlace(MODULE) LEDGER D63), kept here rather than in a host app
 * because publishing new articles to a marketplace is exactly the cross-project reuse this
 * library exists for — not specific to any one ERP.
 *
 * [categoryId] and [attributes] stay marketplace-agnostic in *shape* (plain `String`/`Map`) even
 * though today only MercadoLibre populates them — same escape-hatch precedent as [erpItemId]
 * (D57): a typed, marketplace-specific taxonomy would leak that marketplace's concepts into a
 * multi-marketplace model.
 */
@Serializable
data class MarketplaceListingDraft(
    override val _id: MarketplaceListingDraftId = OId(),
    val marketplaceId: MarketplaceId,
    /** Raw ERP item id (e.g. a SAE Clave/SKU) — same convention as [MarketplaceListing.erpItemId]. */
    val erpItemId: String,
    val title: String,
    val price: Money = Money.ZERO,
    val status: MarketplaceListingDraftStatus = MarketplaceListingDraftStatus.Borrador,
    val categoryId: String? = null,
    /** Human-readable label for [categoryId], cached at confirmation time so callers don't need to re-look it up for display. */
    val categoryName: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val pictureUrls: List<String> = emptyList(),
    val listingTypeId: String? = null,
    val condition: String? = null,
    val rejectionReason: String? = null,
    /** Set once `POST /items` (or equivalent) succeeds and the real listing is created. */
    val publishedListingId: MarketplaceListingId? = null,
) : BaseDoc<MarketplaceListingDraftId>

@Serializable
enum class MarketplaceListingDraftStatus {
    Borrador,
    CategoriaConfirmada,
    Publicado,
    Rechazado,
    Error,
}
