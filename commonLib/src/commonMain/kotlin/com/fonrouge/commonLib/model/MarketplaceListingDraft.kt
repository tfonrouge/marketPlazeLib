package com.fonrouge.commonLib.model

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.datetime.Instant
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
    /**
     * mppErsaPack D82 (ROAR r8) — marca DURABLE del efecto remoto: se fija en cuanto `POST /items`
     * devuelve éxito, **antes** de graduar a un [MarketplaceListing]. Publicar no es idempotente (no hay
     * idempotency key hacia el marketplace ni transacción Mongo), así que si el proceso cae entre el POST
     * y la graduación, un reintento **reanuda** por este id (find-or-create del listing) en vez de
     * re-publicar. `null` = todavía no se hizo el POST (o quedó a medias antes de confirmarse en ML).
     */
    val createdItemId: String? = null,
    /**
     * mppErsaPack D82 (ROAR r14) — inicio del "lease" de publicación: se fija al reclamar el draft
     * (`… → Publicando`). Si el proceso cae ANTES de escribir `createdItemId`/estado, el draft queda
     * `Publicando + createdItemId=null`; sin marca temporal "esperá" ser&iacute;a para siempre. Un
     * `Publicando` cuyo lease venci&oacute; (y sin `createdItemId`) puede TOMARSE (takeover auditado) hacia
     * `RequiereReconciliacion` para cerrarlo. `null` = draft anterior a este campo (se trata como vencido).
     */
    val publishingStartedAt: Instant? = null,
    /**
     * mppErsaPack D82 (ROAR r16) — auditoría DURABLE del takeover de un `Publicando` abandonado (lease
     * vencido → `RequiereReconciliacion`): quién lo tomó y cuándo. Se guarda en el draft (no en
     * `rejectionReason`, que la graduación limpia) y sobrevive al cierre. Distinto de `reconciledBy` del
     * listing (quien luego adopta el `MLM`). `null` si el draft nunca fue tomado por lease vencido.
     */
    val takenOverBy: String? = null,
    val takenOverAt: Instant? = null,
) : BaseDoc<MarketplaceListingDraftId>

@Serializable
enum class MarketplaceListingDraftStatus {
    Borrador,
    CategoriaConfirmada,

    /**
     * mppErsaPack D82 (ROAR r8) — estado de reserva: un publish reclamó el draft (CAS
     * `CategoriaConfirmada → Publicando`) y está por hacer / hizo el `POST /items`. Serializa
     * publicaciones concurrentes (sólo un request gana el CAS) y marca "en curso" para reanudar.
     */
    Publicando,

    /**
     * mppErsaPack D82 (ROAR r12) — resultado **ambiguo** PERSISTIDO: el `POST` dio 5xx/timeout, no se sabe
     * si ML creó el ítem. Distinto de `Publicando` (request activo) — se fija SÓLO cuando el POST ya
     * devolvió ambiguo. Habilita la reconciliación manual (el operador pega el `MLM…` verificado en ML) y
     * evita que la reconciliación curse mientras el POST sigue en vuelo (que lo sobrescribiría).
     */
    RequiereReconciliacion,
    Publicado,
    Rechazado,
    Error,
}
