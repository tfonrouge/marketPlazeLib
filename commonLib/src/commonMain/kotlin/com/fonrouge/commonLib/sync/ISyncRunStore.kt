package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId

/**
 * Historial de sincronizaciones: que se ejecuto, cuando y como termino.
 *
 * Es el puerto que permite contestar «¿cuando fue la ultima conciliacion buena?» y «¿esto lleva
 * fallando desde el jueves?». Sin el, un reporte en memoria contesta lo primero mal y lo segundo
 * nunca.
 *
 * ## Contrato
 *
 * - [record] **no interpreta**: guarda lo que se le da. Decidir si una corrida fue aceptable es del
 *   consumidor, no del almacen.
 * - [last] devuelve la mas reciente por `finishedAt`; con empate, cualquiera de las empatadas.
 * - [history] devuelve de la mas reciente a la mas antigua. `limit` acota; una implementacion no
 *   debe traer el historial entero para descartarlo despues.
 * - Guardar **nunca** puede tirar la corrida que se esta registrando: si la escritura falla, que
 *   falle ruidosamente, pero registrar es observabilidad y no debe convertirse en una via de fallo
 *   de la propia sincronizacion. Quien llame decide; el puerto no lo silencia por su cuenta.
 */
interface ISyncRunStore {

    /** Deja constancia de una corrida terminada. */
    suspend fun record(run: SyncRun)

    /** La ultima corrida de ese tipo, o `null` si nunca hubo. */
    suspend fun last(marketplaceId: MarketplaceId, kind: SyncKind): SyncRun?

    /** Historial descendente por fecha, acotado. */
    suspend fun history(marketplaceId: MarketplaceId, kind: SyncKind? = null, limit: Int = 50): List<SyncRun>
}
