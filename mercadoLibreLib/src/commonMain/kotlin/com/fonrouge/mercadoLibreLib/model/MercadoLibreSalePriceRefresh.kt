package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.datetime.Instant

/**
 * Estados de publicación cuyo precio de venta se consulta. `closed` y los demás no están a la venta:
 * consultarlos gasta llamadas sin dar un precio que alguien pague. Un estado nuevo de ML queda fuera
 * hasta que alguien decida incluirlo.
 */
val SALE_PRICE_ELIGIBLE_STATUSES: Set<String> = setOf("active", "paused")

/**
 * Qué publicaciones consultar en este ciclo, en orden: las nunca consultadas, luego las que no quedaron
 * vigentes, luego la de intento más viejo. El [quota] se aplica **después** de ordenar, así una clase
 * numerosa no desplaza a una de mayor prioridad. Puro.
 */
fun planSalePriceRefresh(
    products: List<MercadoLibreProduct>,
    previous: Map<String, MercadoLibreSalePriceObservation>,
    quota: Int,
): List<String> = products
    .filter { it.status.lowercase() in SALE_PRICE_ELIGIBLE_STATUSES }
    .map { it._id.id }
    .distinct()
    .sortedWith(
        compareBy<String>(
            { if (previous[it] == null) 0 else 1 },
            { if (previous[it]?.status == MercadoLibreSalePriceStatus.OBSERVED) 1 else 0 },
            { previous[it]?.lastAttemptAt },
            { it },
        )
    )
    .take(quota.coerceAtLeast(0))

/** Resumen de un ciclo de refresco, para el log. */
data class SalePriceRefreshReport(
    val planned: Int = 0,
    val observed: Int = 0,
    val withPromotion: Int = 0,
    /** Publicaciones sin promoción cuyo precio de venta no es el publicado: debería estar vacía. */
    val noPromotionDifferent: List<String> = emptyList(),
    val notAvailable: Int = 0,
    val unreadable: Int = 0,
    val retryable: Int = 0,
    /** Por qué se cortó el ciclo antes de terminar; `null` si recorrió todo lo planeado. */
    val stoppedBy: String? = null,
) {
    override fun toString(): String =
        "planeadas=$planned observadas=$observed conPromocion=$withPromotion " +
            "sinPromocionDistinto=${noPromotionDifferent.size}${if (noPromotionDifferent.isEmpty()) "" else noPromotionDifferent} " +
            "noDisponibles=$notAvailable ilegibles=$unreadable transitorios=$retryable" +
            (stoppedBy?.let { " CORTE: $it" } ?: "")
}

/**
 * El ciclo de refresco sin I/O: por cada respuesta decide qué guardar y si cortar. El anfitrión sólo hace
 * la llamada y la escritura.
 *
 * Corta en la **primera** respuesta 401/403 sin escribir nada — el token no se refresca desde aquí, y seguir
 * sólo convertiría cada publicación en un error falso — y tras [maxConsecutiveRetryable] fallos
 * transitorios seguidos, que es el síntoma de una caída y no de un ítem suelto.
 *
 * @param listed los productos del ciclo por id, para contrastar el precio de venta con el publicado.
 */
class SalePriceRefreshCycle(
    planned: Int,
    private val listed: Map<String, MercadoLibreProduct>,
    private val maxConsecutiveRetryable: Int = 5,
) {
    data class Step(val toSave: MercadoLibreSalePriceObservation?, val stop: Boolean)

    var report: SalePriceRefreshReport = SalePriceRefreshReport(planned = planned)
        private set

    private var consecutiveRetryable = 0

    fun record(
        previous: MercadoLibreSalePriceObservation?,
        itemId: String,
        marketplaceId: MarketplaceId,
        outcome: MercadoLibreSalePriceFetch,
        now: Instant,
    ): Step {
        val next = MercadoLibreSalePriceObservation.next(previous, itemId, marketplaceId, outcome, now)
        if (next == null) {
            report = report.copy(stoppedBy = "token rechazado (401/403) en $itemId; este ciclo no lo refresca")
            return Step(toSave = null, stop = true)
        }
        if (next.status == MercadoLibreSalePriceStatus.RETRYABLE_ERROR) {
            consecutiveRetryable++
            report = report.copy(retryable = report.retryable + 1)
            if (consecutiveRetryable >= maxConsecutiveRetryable) {
                report = report.copy(stoppedBy = "$consecutiveRetryable fallos transitorios seguidos (último en $itemId)")
                return Step(toSave = next, stop = true)
            }
            return Step(toSave = next, stop = false)
        }
        consecutiveRetryable = 0
        report = when (next.status) {
            MercadoLibreSalePriceStatus.OBSERVED -> {
                val price = next.salePrice
                val product = listed[itemId]
                val differs = price != null && product != null &&
                    price.compareWithListed(product.price, product.currencyId) == SalePriceVsListed.NO_PROMOTION_DIFFERENT
                report.copy(
                    observed = report.observed + 1,
                    withPromotion = report.withPromotion + if (price?.hasPromotion == true) 1 else 0,
                    noPromotionDifferent = if (differs) report.noPromotionDifferent + itemId else report.noPromotionDifferent,
                )
            }
            MercadoLibreSalePriceStatus.NOT_AVAILABLE -> report.copy(notAvailable = report.notAvailable + 1)
            MercadoLibreSalePriceStatus.UNREADABLE -> report.copy(unreadable = report.unreadable + 1)
            MercadoLibreSalePriceStatus.RETRYABLE_ERROR -> report
        }
        return Step(toSave = next, stop = false)
    }
}
