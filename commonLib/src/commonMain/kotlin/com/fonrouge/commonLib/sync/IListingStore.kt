package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListing

/**
 * Donde viven los listings entre una conciliacion y la siguiente.
 *
 * ## Por que es un puerto y no una implementacion
 *
 * La libreria necesita **leer y escribir** listings, pero no puede decidir donde: un consumidor los
 * guarda en Mongo, otro en Postgres, los ejemplos en memoria. Definir aqui la forma y dejar la
 * persistencia al consumidor es lo que mantiene la libreria agnostica del ERP y de la base.
 *
 * ## Que problema resuelve
 *
 * Un consumidor real los tenia en un `ConcurrentHashMap` estatico. Al reiniciar el servidor
 * desaparecian, y como el empuje de inventario cruza SKU contra listings cacheados, tras cada
 * reinicio no habia nada que empujar hasta volver a conciliar — sin que nada lo dijera. Un mapa en
 * memoria es una decision de persistencia tomada por omision; este puerto la vuelve explicita.
 *
 * ## Contrato
 *
 * - Todo esta **acotado por marketplace**. Un `externalId` sólo es único dentro del suyo, y mezclar
 *   dos marketplaces es la clase de error que no avisa.
 * - [upsert] identifica por `marketplaceId` + `externalId`, no por `_id`: el `_id` local puede no
 *   existir todavia la primera vez que se ve un listing remoto.
 * - **No hay borrado masivo a proposito.** Un listing que desaparece del remoto es informacion, no
 *   basura, y decidir que hacer con el es politica del consumidor.
 *
 * Una implementacion se valida contra `ListingStoreContract` en los tests de esta libreria.
 */
interface IListingStore {

    /** Todos los listings conocidos de un marketplace. Lista vacia si no hay ninguno. */
    suspend fun listings(marketplaceId: MarketplaceId): List<MarketplaceListing>

    /** El listing con ese `externalId` dentro del marketplace, o `null`. */
    suspend fun byExternalId(marketplaceId: MarketplaceId, externalId: String): MarketplaceListing?

    /**
     * Inserta o reemplaza por (`marketplaceId`, `externalId`).
     *
     * Debe ser idempotente: guardar dos veces el mismo listing deja **uno**.
     */
    suspend fun upsert(listing: MarketplaceListing)

    /**
     * [upsert] de varios. Existe porque una conciliacion completa trae miles y hacerlos de uno en
     * uno es la diferencia entre segundos y minutos.
     *
     * No es transaccional salvo que la implementacion lo diga: es un lote, no un todo-o-nada.
     */
    suspend fun upsertAll(listings: List<MarketplaceListing>) {
        listings.forEach { upsert(it) }
    }

    /** Cuantos listings hay. Para decidir si una conciliacion partio de algo o de cero. */
    suspend fun count(marketplaceId: MarketplaceId): Int = listings(marketplaceId).size

    /**
     * Elimina un listing concreto. Sin version masiva **a proposito**: un borrado en bloque sobre
     * datos que reflejan un remoto es justamente la operacion que no debe estar a un descuido de
     * distancia.
     */
    suspend fun delete(marketplaceId: MarketplaceId, externalId: String)
}
