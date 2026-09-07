package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId

/**
 * Marcas de avance para sincronizacion incremental — cursores de paginacion, «modificado desde».
 *
 * ## Por que un almacen aparte y no un campo en [SyncRun]
 *
 * Son cosas distintas y se corrompen de forma distinta. Una corrida es un **hecho pasado** que no
 * cambia; un checkpoint es **estado vivo** que se pisa cada vez. Guardarlos juntos invita a
 * reconstruir el punto de avance leyendo el historial, y entonces una corrida mal registrada se
 * convierte en datos que no se vuelven a leer.
 *
 * ## Contrato
 *
 * - La clave es (`marketplaceId`, `key`). El `key` lo elige la integracion — `"products.updatedAt"`,
 *   `"collections.cursor"`— y **no debe** incluir el marketplace: ya va aparte.
 * - El valor es una cadena opaca. El puerto no la interpreta y no debe intentarlo: hoy es un cursor
 *   de Shopify, mañana un ISO-8601 de MercadoLibre.
 * - [clear] existe para poder **forzar una pasada completa**. Es la salida cuando un incremental se
 *   desincroniza, y sin ella la unica alternativa es tocar la base a mano.
 * - Guardar el checkpoint **despues** de que el trabajo haya tenido efecto, nunca antes: al reves,
 *   un fallo a mitad deja el cursor avanzado y los elementos de ese tramo no se vuelven a mirar.
 *   El puerto no puede imponerlo, pero es la razon de que exista escrito aqui.
 */
interface ISyncCheckpointStore {

    /** El valor guardado, o `null` si nunca se guardo — que significa «empezar desde el principio». */
    suspend fun get(marketplaceId: MarketplaceId, key: String): String?

    /** Guarda o reemplaza. */
    suspend fun set(marketplaceId: MarketplaceId, key: String, value: String)

    /** Borra la marca, forzando que la proxima pasada sea completa. */
    suspend fun clear(marketplaceId: MarketplaceId, key: String)
}
