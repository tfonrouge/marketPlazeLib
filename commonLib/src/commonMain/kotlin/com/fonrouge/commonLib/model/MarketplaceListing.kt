package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.datetime.Instant
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
    /**
     * D81 (H3 híbrido) — CONTROL fail-closed del vínculo listing→artículo (`erpItemId`), que decide
     * qué se factura. `false` = un humano aún no verificó este binding; el pipeline de órdenes lo
     * BLOQUEA (route a Revisar) hasta que se confirme. **Default `false` a propósito**: un campo
     * ausente significa "no sabemos cómo nació este binding", NO "confirmado" — ausencia de evidencia
     * no puede equivaler a confirmación. Los docs previos a este campo (procedencia [MlLinkSource.LEGACY])
     * requieren backfill EXPLÍCITO con evidencia real de procedencia, no un default permisivo.
     */
    val linkConfirmed: Boolean = false,
    /** Procedencia del binding (auditoría). Ver [MlLinkSource]. */
    val linkSource: MlLinkSource = MlLinkSource.LEGACY,
    /** Cuándo se confirmó el binding (null mientras `linkConfirmed=false`). */
    val confirmedAt: Instant? = null,
    /**
     * Quién confirmó: identidad autenticada (del `ApplicationCall`) en confirmación/ligado manual;
     * marcador de sistema explícito (`system:publication`, `migration:...`) en publicación/backfill.
     * Nunca texto libre ni vacío.
     */
    val confirmedBy: String? = null,
    /**
     * mppErsaPack D82 (ROAR r13) — auditoría de la **RECONCILIACIÓN manual** (distinta de la confirmación):
     * quién adoptó este `MLM…` para un draft cuya publicación quedó ambigua, y cuándo. La confirmación
     * ([confirmedBy]/[confirmedAt]) registra a quien HABILITA la facturación después; ésta registra a quien
     * resolvió la ambigüedad remota. `null` en bindings no reconciliados.
     */
    val reconciledBy: String? = null,
    val reconciledAt: Instant? = null,
) : BaseDoc<MarketplaceListingId>
