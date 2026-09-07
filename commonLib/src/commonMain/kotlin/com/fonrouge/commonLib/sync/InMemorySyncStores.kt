package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListing

/**
 * Implementaciones en memoria de los tres puertos de sincronizacion.
 *
 * ## Para que sirven, y para que no
 *
 * Sirven para pruebas, para los ejemplos y como **referencia ejecutable del contrato**: si una
 * implementacion real se comporta distinto a estas en `SyncStoreContractTest`, la que esta mal es
 * la real.
 *
 * **No sirven en produccion**, y conviene decirlo aqui porque el error que motivo estos puertos fue
 * exactamente ese: un consumidor guardaba los listings en un `ConcurrentHashMap` estatico, que no es
 * mas que esto sin haberlo decidido. Un almacen en memoria pierde todo al reiniciar; la diferencia
 * entre este y aquel es que este lo dice en su nombre.
 *
 * No son seguras para uso concurrente: envuelvelas si las compartes entre corrutinas.
 */
class InMemoryListingStore : IListingStore {

    /** Clave compuesta: un `externalId` sólo es único dentro de su marketplace. */
    private val porClave = LinkedHashMap<Pair<String, String>, MarketplaceListing>()

    private fun clave(marketplaceId: MarketplaceId, externalId: String) =
        marketplaceId.toString() to externalId

    override suspend fun listings(marketplaceId: MarketplaceId): List<MarketplaceListing> =
        porClave.values.filter { it.marketplaceId == marketplaceId }

    override suspend fun byExternalId(marketplaceId: MarketplaceId, externalId: String): MarketplaceListing? =
        porClave[clave(marketplaceId, externalId)]

    override suspend fun upsert(listing: MarketplaceListing) {
        porClave[clave(listing.marketplaceId, listing.externalId)] = listing
    }

    override suspend fun delete(marketplaceId: MarketplaceId, externalId: String) {
        porClave.remove(clave(marketplaceId, externalId))
    }
}

/** Historial en memoria. Ver la advertencia de [InMemoryListingStore]. */
class InMemorySyncRunStore : ISyncRunStore {

    private val corridas = mutableListOf<SyncRun>()

    override suspend fun record(run: SyncRun) {
        corridas += run
    }

    override suspend fun last(marketplaceId: MarketplaceId, kind: SyncKind): SyncRun? =
        corridas.filter { it.marketplaceId == marketplaceId && it.kind == kind }
            .maxByOrNull { it.finishedAt }

    override suspend fun history(marketplaceId: MarketplaceId, kind: SyncKind?, limit: Int): List<SyncRun> =
        corridas.asSequence()
            .filter { it.marketplaceId == marketplaceId && (kind == null || it.kind == kind) }
            .sortedByDescending { it.finishedAt }
            .take(limit)
            .toList()
}

/** Checkpoints en memoria. Ver la advertencia de [InMemoryListingStore]. */
class InMemorySyncCheckpointStore : ISyncCheckpointStore {

    private val valores = mutableMapOf<Pair<String, String>, String>()

    override suspend fun get(marketplaceId: MarketplaceId, key: String): String? =
        valores[marketplaceId.toString() to key]

    override suspend fun set(marketplaceId: MarketplaceId, key: String, value: String) {
        valores[marketplaceId.toString() to key] = value
    }

    override suspend fun clear(marketplaceId: MarketplaceId, key: String) {
        valores.remove(marketplaceId.toString() to key)
    }
}
