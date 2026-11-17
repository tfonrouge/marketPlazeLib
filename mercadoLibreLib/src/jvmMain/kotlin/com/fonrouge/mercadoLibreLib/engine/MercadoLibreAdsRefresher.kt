package com.fonrouge.mercadoLibreLib.engine

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsCycle
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsCycleReport
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayStatus
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsIdentity
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSavedState
import com.fonrouge.mercadoLibreLib.model.planAdsReads
import com.fonrouge.mercadoLibreLib.model.runAdsCycle
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/** Dónde guarda el ciclo los días: Mongo en el anfitrión ([com.fonrouge.mercadoLibreLib.colls.IMercadoLibreAdsColl]),
 * memoria en las pruebas. Escribe sólo documentos completos (B1 R1). */
interface MercadoLibreAdsDayStore {
    suspend fun savedStates(identity: MercadoLibreAdsIdentity): Map<String, MercadoLibreAdsSavedState>
    suspend fun load(identity: MercadoLibreAdsIdentity, date: String): MercadoLibreAdsDay?

    /**
     * Escribe el documento entero **sólo si el guardado sigue siendo [expected]**: misma revisión y misma última lectura;
     * con [expected] `null`, sólo si todavía no existe. Devuelve `false` si otro escritor llegó antes (relectura manual,
     * importación del historial u otro ciclo): lo suyo queda intacto. Nunca un `$set` de un campo suelto.
     */
    suspend fun replaceIf(day: MercadoLibreAdsDay, expected: MercadoLibreAdsDay?): Boolean
}

/**
 * Un ciclo de descarga de Product Ads (B2 S4): interruptor, plan, cierre de días vencidos y lectura con cupo.
 * - Con [refresh] `enabledFrom = null` no toca nada: ni el almacén ni la red (R16).
 * - El cupo es de solicitudes HTTP ([MercadoLibreAdsCycle]); entre una y otra espera [pauseMillis].
 * - El token sale del [Marketplace] que pasa el anfitrión, releído en cada ciclo, y nunca se refresca (R14, D42).
 */
class MercadoLibreAdsRefresher(
    private val client: MercadoLibreAdsClient,
    private val store: MercadoLibreAdsDayStore,
) {
    suspend fun refresh(
        marketplace: Marketplace,
        identity: MercadoLibreAdsIdentity,
        enabledFrom: String?,
        today: LocalDate,
        hourMexicoCity: Int,
        now: String,
        rereadDays: Int,
        quotaRequests: Int,
        pauseMillis: Long = DEFAULT_PAUSE_MILLIS,
        windowDays: Int = 90,
    ): MercadoLibreAdsCycleReport {
        if (enabledFrom == null) return MercadoLibreAdsCycleReport(skippedBy = "interruptor apagado")
        val plan = planAdsReads(today, hourMexicoCity, enabledFrom, store.savedStates(identity), windowDays, rereadDays)
        val closed = mutableListOf<String>()
        val conflicts = mutableListOf<String>()
        for (date in plan.close) {
            val saved = store.load(identity, date) ?: continue
            if (store.replaceIf(saved.copy(status = MercadoLibreAdsDayStatus.CERRADO), saved)) closed += date else conflicts += date
        }
        var calls = 0
        val report = runAdsCycle(
            MercadoLibreAdsCycle(plan.reads, quotaRequests), now, identity,
            get = { request ->
                if (calls++ > 0 && pauseMillis > 0) delay(pauseMillis)
                client.get(marketplace, identity.advertiserId, request)
            },
            load = { store.load(identity, it) },
            store = { day, expected -> store.replaceIf(day, expected) },
        )
        return report.copy(closed = closed, conflicts = conflicts + report.conflicts)
    }

    companion object {
        /** La pausa de la sonda que leyó producción sin un solo 429 (L-019). */
        const val DEFAULT_PAUSE_MILLIS = 1_000L
    }
}
