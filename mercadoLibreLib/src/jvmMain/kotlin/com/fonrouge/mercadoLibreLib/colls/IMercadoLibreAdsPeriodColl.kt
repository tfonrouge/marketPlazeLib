package com.fonrouge.mercadoLibreLib.colls

import com.fonrouge.base.types.StringId
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.mercadoLibreLib.common.CommonMercadoLibreAdsPeriod
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayFilter
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsPeriod
import org.litote.kmongo.coroutine.CoroutineCollection

/**
 * Periodos de Mercado Ads sin granularidad diaria (`mercadoLibreAdsPeriod`, PublicidadMercadoAds B1 R20 y R21). Sólo los
 * escribe el importador del historial (B4-12), una vez y con la huella del archivo; el ciclo de la API no los toca.
 */
abstract class IMercadoLibreAdsPeriodColl<UID : Any>(
    debug: Boolean = false,
) : Coll<MercadoLibreAdsPeriod, StringId<MercadoLibreAdsPeriod>, MercadoLibreAdsDayFilter, UID>(
    commonContainer = CommonMercadoLibreAdsPeriod,
    debug = debug,
) {
    /** El importador busca traslapes por cuenta, producto, anunciante e intervalo (R23). */
    override suspend fun CoroutineCollection<MercadoLibreAdsPeriod>.indexes() {
        ensureIndex(MercadoLibreAdsPeriod::marketplaceId, MercadoLibreAdsPeriod::product, MercadoLibreAdsPeriod::advertiserId, MercadoLibreAdsPeriod::from)
    }
}
