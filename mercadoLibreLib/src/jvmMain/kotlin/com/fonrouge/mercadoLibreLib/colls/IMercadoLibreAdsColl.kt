package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsClient
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsDayStore
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsRefresher
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsCycleReport
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayFilter
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsIdentity
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSavedState
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import org.litote.kmongo.coroutine.CoroutineCollection
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.litote.kmongo.and
import org.litote.kmongo.eq

/**
 * Días de Product Ads (`mercadoLibreAdsDay`, PublicidadMercadoAds B1) y el ciclo que los descarga ([refreshAds]). Es el
 * **único escritor** de la colección, y escribe sólo documentos completos con `replaceOne` (R1). La lógica del ciclo es
 * pura y vive en `MercadoLibreAds.kt`; aquí sólo están el almacén y la llamada.
 */
abstract class IMercadoLibreAdsColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreAdsDay, StringId<MercadoLibreAdsDay>, MercadoLibreAdsDayFilter, UID>(
    commonContainer = CommonMercadoLibreAdsDay,
    debug = debug,
), MercadoLibreAdsDayStore {

    /** El cliente HTTP. El anfitrión no lo cambia; las pruebas, sí. */
    open val adsClient: MercadoLibreAdsClient = MercadoLibreAdsClient()

    /**
     * Un ciclo. Con [enabledFrom] `null` (interruptor apagado) no hace ninguna llamada ni lee la colección (R16). El
     * anfitrión decide [quotaRequests] (L-025) y [rereadDays] (L-012), y pasa el [marketplace] recién leído (R14).
     */
    suspend fun refreshAds(
        marketplace: Marketplace,
        identity: MercadoLibreAdsIdentity,
        enabledFrom: String?,
        today: LocalDate,
        hourMexicoCity: Int,
        now: String,
        rereadDays: Int,
        quotaRequests: Int,
        pauseMillis: Long = MercadoLibreAdsRefresher.DEFAULT_PAUSE_MILLIS,
    ): MercadoLibreAdsCycleReport = MercadoLibreAdsRefresher(adsClient, this).refresh(
        marketplace, identity, enabledFrom, today, hourMexicoCity, now, rereadDays, quotaRequests, pauseMillis,
    )

    override suspend fun savedStates(identity: MercadoLibreAdsIdentity): Map<String, MercadoLibreAdsSavedState> =
        coroutine.find(
            and(
                MercadoLibreAdsDay::marketplaceId eq identity.marketplaceId,
                MercadoLibreAdsDay::product eq identity.product,
                MercadoLibreAdsDay::advertiserId eq identity.advertiserId,
            )
        ).toList().associate { it.date to MercadoLibreAdsSavedState(it.status, it.source, lastReadDateMexicoCity(it.lastReadAt)) }

    override suspend fun load(identity: MercadoLibreAdsIdentity, date: String): MercadoLibreAdsDay? =
        coroutine.find(BaseDoc<StringId<MercadoLibreAdsDay>>::_id eq StringId(MercadoLibreAdsDay.idOf(identity, date))).first()

    /**
     * Escritura condicionada (B1 R1 y R7): reemplaza el documento sólo si sigue en la revisión y la última lectura que se
     * leyeron; si no existía, lo inserta sólo si nadie lo insertó antes (llave duplicada → `false`).
     */
    override suspend fun replaceIf(day: MercadoLibreAdsDay, expected: MercadoLibreAdsDay?): Boolean {
        if (expected == null) {
            return try {
                coroutine.insertOne(day)
                true
            } catch (e: MongoWriteException) {
                if (e.error.category == ErrorCategory.DUPLICATE_KEY) false else throw e
            }
        }
        val result = coroutine.replaceOne(
            and(
                BaseDoc<StringId<MercadoLibreAdsDay>>::_id eq day._id,
                MercadoLibreAdsDay::revision eq expected.revision,
                MercadoLibreAdsDay::lastReadAt eq expected.lastReadAt,
            ),
            day,
        )
        return result.matchedCount == 1L
    }

    /** Lo que consulta el plan en cada ciclo: los días de una cuenta, producto y anunciante. */
    override suspend fun CoroutineCollection<MercadoLibreAdsDay>.indexes() {
        ensureIndex(MercadoLibreAdsDay::marketplaceId, MercadoLibreAdsDay::product, MercadoLibreAdsDay::advertiserId, MercadoLibreAdsDay::date)
    }

    companion object {
        /** Fecha de Ciudad de México de la última lectura. Ilegible → «nunca», y el plan la vuelve a leer (lado seguro). */
        fun lastReadDateMexicoCity(lastReadAt: String): String =
            runCatching { Instant.parse(lastReadAt).toLocalDateTime(TimeZone.of("America/Mexico_City")).date.toString() }
                .getOrDefault("0000-00-00")
    }
}
