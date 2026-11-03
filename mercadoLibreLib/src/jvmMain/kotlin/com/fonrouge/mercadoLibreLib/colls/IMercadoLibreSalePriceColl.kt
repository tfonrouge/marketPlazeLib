package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreSalePrice
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePrice
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceFetch
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceFilter
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceObservation
import com.fonrouge.mercadoLibreLib.model.SalePriceRefreshCycle
import com.fonrouge.mercadoLibreLib.model.SalePriceRefreshReport
import com.fonrouge.mercadoLibreLib.model.planSalePriceRefresh
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.litote.kmongo.eq
import org.litote.kmongo.`in`

/**
 * Observaciones del precio de venta de las publicaciones de ML (`mercadoLibreSalePriceObservation`) y el
 * ciclo que las refresca. La librería decide qué consultar, qué guardar y cuándo cortar
 * ([SalePriceRefreshCycle]); el anfitrión pone la llamada HTTP ([fetchSalePrice]), como hace con
 * [IMercadoLibreProductColl.fetchProducts].
 *
 * Es el **único escritor** de la colección; el sync de productos no la toca.
 */
abstract class IMercadoLibreSalePriceColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreSalePriceObservation, StringId<MercadoLibreSalePriceObservation>, MercadoLibreSalePriceFilter, UID>(
    commonContainer = CommonMercadoLibreSalePrice,
    debug = debug,
) {

    /**
     * Una llamada a `GET /items/{itemId}/sale_price?context={context}` con el token vigente de
     * [marketplace].
     *
     * **Contrato: nunca refresca el token.** Un 401/403 se devuelve como
     * [MercadoLibreSalePriceFetch.Unauthorized] y el ciclo se corta ahí; el `refresh_token` de ML es de un
     * solo uso y lo rota quien el anfitrión haya decidido, no un ciclo secundario. Cualquier otro fallo se
     * devuelve como [MercadoLibreSalePriceFetch.Retryable].
     */
    abstract suspend fun fetchSalePrice(
        marketplace: Marketplace,
        itemId: String,
        context: String,
    ): MercadoLibreSalePriceFetch

    /**
     * Un ciclo: planea con [planSalePriceRefresh] sobre los [products] de [marketplace], consulta en orden
     * con una pausa entre llamadas, guarda cada observación como documento completo y se detiene cuando
     * [SalePriceRefreshCycle] lo indica.
     */
    suspend fun refreshSalePrices(
        marketplace: Marketplace,
        products: List<MercadoLibreProduct>,
        quota: Int = DEFAULT_QUOTA,
        pauseMillis: Long = DEFAULT_PAUSE_MILLIS,
    ): SalePriceRefreshReport {
        val own = products.filter { it.marketplaceId == marketplace._id }
        if (own.isEmpty()) return SalePriceRefreshReport()

        val previous = coroutine
            .find(BaseDoc<StringId<MercadoLibreSalePriceObservation>>::_id `in` own.map { StringId<MercadoLibreSalePriceObservation>(it._id.id) })
            .toList()
            .associateBy { it._id.id }
        val plan = planSalePriceRefresh(own, previous, quota)
        val cycle = SalePriceRefreshCycle(planned = plan.size, listed = own.associateBy { it._id.id })

        for ((index, itemId) in plan.withIndex()) {
            if (index > 0) delay(pauseMillis)
            val outcome = try {
                fetchSalePrice(marketplace, itemId, MercadoLibreSalePrice.CONTEXT_MARKETPLACE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MercadoLibreSalePriceFetch.Retryable(e.message ?: e::class.simpleName ?: "error")
            }
            val step = cycle.record(previous[itemId], itemId, marketplace._id, outcome, Clock.System.now())
            step.toSave?.let { save(it) }
            if (step.stop) break
        }
        return cycle.report
    }

    /** Documento completo, nunca `$set` de un campo suelto: los `Instant` quedan con un solo tipo BSON. */
    private suspend fun save(observation: MercadoLibreSalePriceObservation) {
        coroutine.replaceOne(
            BaseDoc<StringId<MercadoLibreSalePriceObservation>>::_id eq observation._id,
            observation,
            ReplaceOptions().upsert(true),
        )
    }

    companion object {
        /** Holgado para el catálogo de hoy (~50 publicaciones); con más, el plan rota por antigüedad. */
        const val DEFAULT_QUOTA = 200
        const val DEFAULT_PAUSE_MILLIS = 250L
    }
}
