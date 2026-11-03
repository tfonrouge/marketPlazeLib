package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resultado de **una** llamada a `GET /items/{id}/sale_price`. Distingue desenlaces que no significan lo
 * mismo: que ML no tenga precio para el ítem es un hecho; que la llamada falle es ruido; que el token no
 * sirva es un problema operativo que debe **verse**, nunca auto-curarse desde aquí.
 */
sealed class MercadoLibreSalePriceFetch {
    data class Found(val body: String) : MercadoLibreSalePriceFetch()

    /** 404: ML no da precio de venta para ese ítem. */
    data object NotFound : MercadoLibreSalePriceFetch()

    /**
     * 401/403: el token no sirve. Quien consulta **no lo refresca**: el `refresh_token` de ML es de un solo
     * uso y rotarlo desde un ciclo secundario compite con el rotador del anfitrión.
     */
    data object Unauthorized : MercadoLibreSalePriceFetch()

    /** 429, 5xx, red, cuerpo vacío: no concluyente, se reintenta en el siguiente ciclo. */
    data class Retryable(val reason: String) : MercadoLibreSalePriceFetch()

    companion object {
        fun classify(status: Int, body: String?): MercadoLibreSalePriceFetch = when {
            status == 200 && !body.isNullOrBlank() -> Found(body)
            status == 200 -> Retryable("200 con cuerpo vacío")
            status == 404 -> NotFound
            status == 401 || status == 403 -> Unauthorized
            else -> Retryable("HTTP $status")
        }
    }
}

/** Estado del último intento de observar el precio de venta de una publicación. */
@Serializable
enum class MercadoLibreSalePriceStatus {
    /** ML devolvió un precio legible: es el de [MercadoLibreSalePriceObservation.observedAt]. */
    @SerialName("observed")
    OBSERVED,

    /** ML respondió 404: no hay precio de venta para el ítem. */
    @SerialName("not_available")
    NOT_AVAILABLE,

    /** ML respondió, pero sin un precio interpretable (ver `lastError`). */
    @SerialName("unreadable")
    UNREADABLE,

    /** Falló la llamada (429, 5xx, red); se reintenta. */
    @SerialName("retryable_error")
    RETRYABLE_ERROR,
}

/**
 * Lo último que se sabe del precio de venta de una publicación (colección
 * `mercadoLibreSalePriceObservation`, `_id` = el id del ítem, el mismo `MLM…` que [MercadoLibreProduct]).
 *
 * Vive aparte del producto porque el sync de productos hace `$set` del documento completo en cada corrida y
 * pisaría un campo que escribe otro ciclo: aquí hay un solo escritor. Se lee hidratado en
 * [MercadoLibreProduct.salePrice].
 *
 * Un fallo **no borra** el último precio: conserva [salePrice] y [observedAt] y cambia [status]. Sólo
 * [isCurrent] dice que el precio es de hoy. Se escribe siempre como documento completo (nunca `$set` de un
 * `Instant` suelto), así los instantes quedan con un solo tipo BSON.
 */
@Serializable
data class MercadoLibreSalePriceObservation(
    override val _id: StringId<MercadoLibreSalePriceObservation>,
    val marketplaceId: MarketplaceId,
    val context: String = MercadoLibreSalePrice.CONTEXT_MARKETPLACE,
    val status: MercadoLibreSalePriceStatus,
    /** El último precio obtenido; se conserva ante fallos posteriores. */
    val salePrice: MercadoLibreSalePrice? = null,
    /** Cuándo se obtuvo [salePrice]. */
    val observedAt: Instant? = null,
    val lastAttemptAt: Instant,
    val lastError: String? = null,
) : BaseDoc<StringId<MercadoLibreSalePriceObservation>> {

    /** El último intento obtuvo el precio: es el vigente. Cualquier otro estado es una observación pasada. */
    val isCurrent: Boolean get() = status == MercadoLibreSalePriceStatus.OBSERVED && salePrice != null

    companion object {
        /**
         * Transición pura: qué guardar tras una llamada. `null` = no escribir nada, que es lo que pasa con un
         * token rechazado (el ciclo se corta ahí y la observación anterior queda intacta).
         */
        fun next(
            previous: MercadoLibreSalePriceObservation?,
            itemId: String,
            marketplaceId: MarketplaceId,
            outcome: MercadoLibreSalePriceFetch,
            now: Instant,
        ): MercadoLibreSalePriceObservation? {
            val base = MercadoLibreSalePriceObservation(
                _id = StringId(itemId),
                marketplaceId = marketplaceId,
                status = MercadoLibreSalePriceStatus.RETRYABLE_ERROR,
                salePrice = previous?.salePrice,
                observedAt = previous?.observedAt,
                lastAttemptAt = now,
            )
            return when (outcome) {
                is MercadoLibreSalePriceFetch.Unauthorized -> null
                is MercadoLibreSalePriceFetch.NotFound -> base.copy(status = MercadoLibreSalePriceStatus.NOT_AVAILABLE)
                is MercadoLibreSalePriceFetch.Retryable -> base.copy(lastError = outcome.reason)
                is MercadoLibreSalePriceFetch.Found -> when (val parsed = MercadoLibreSalePrice.parse(outcome.body)) {
                    is SalePriceParse.Parsed -> base.copy(
                        status = MercadoLibreSalePriceStatus.OBSERVED,
                        salePrice = parsed.salePrice,
                        observedAt = now,
                    )
                    is SalePriceParse.Unreadable -> base.copy(
                        status = MercadoLibreSalePriceStatus.UNREADABLE,
                        lastError = parsed.reason,
                    )
                }
            }
        }
    }
}
