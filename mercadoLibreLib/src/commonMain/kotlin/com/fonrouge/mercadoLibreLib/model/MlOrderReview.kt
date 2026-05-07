package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.annotations.Collection
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Operational review state for a MercadoLibre order before it advances downstream
 * (e.g., into a host-app pedido or invoice).
 *
 * The pipeline is two-staged:
 *   1. ML order arrives via sync — an MlOrderReview is created with appStatus=Revisar.
 *   2. Operator reviews and either Aceptado (→ host-app processing) or Rechazado
 *      (→ optionally notified to ML with a causa).
 *
 * [_id] matches the [MercadoLibreOrder]._id (ML order ID as String).
 *
 * The state machine and audit fields live in this lib so any host app that consumes
 * `MercadoLibreOrder` can reuse the review concept and benefit from server-side
 * lookup pipelines on the order collection.
 */
@Serializable
@Collection(name = "mlOrderReview")
data class MlOrderReview(
    override val _id: StringId<MlOrderReview>,
    val mlOrderId: String,
    val marketplaceId: String,
    val appStatus: AppStatus = AppStatus.Revisar,
    val dateCreated: Instant = Clock.System.now(),
    val fechaRevision: Instant? = null,
    val revisedBy: String? = null,
    val revisionMode: RevisionMode? = null,
    val causaRechazo: String? = null,
    /**
     * Operational note attached during automatic transitions (e.g., processing detected
     * the buyer was unmapped after acceptance). Free-form text shown to the operator
     * on the review item view.
     */
    val revisionNote: String? = null,
    val mlNotificationStatus: MlNotificationStatus = MlNotificationStatus.NoEnviado,
    val mlNotificationError: String? = null,
    val pedidoClienteHeadId: String? = null,
) : BaseDoc<StringId<MlOrderReview>> {

    @Serializable
    enum class AppStatus {
        @SerialName("R")
        Revisar,

        @SerialName("A")
        Aceptado,

        @SerialName("X")
        Rechazado,

        @SerialName("P")
        Procesado,
    }

    @Serializable
    enum class RevisionMode {
        @SerialName("M")
        Manual,

        @SerialName("A")
        Auto,
    }

    @Serializable
    enum class MlNotificationStatus {
        @SerialName("N")
        NoEnviado,

        @SerialName("E")
        Enviado,

        @SerialName("F")
        Fallido,
    }
}