import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.MarketplaceListing
import com.fonrouge.commonLib.sync.IListingStore
import com.fonrouge.commonLib.sync.ISyncCheckpointStore
import com.fonrouge.commonLib.sync.ISyncRunStore
import com.fonrouge.commonLib.sync.InMemoryListingStore
import com.fonrouge.commonLib.sync.InMemorySyncCheckpointStore
import com.fonrouge.commonLib.sync.InMemorySyncRunStore
import com.fonrouge.commonLib.sync.SyncKind
import com.fonrouge.commonLib.sync.SyncOutcome
import com.fonrouge.commonLib.sync.SyncRun
import com.fonrouge.commonLib.sync.SyncRunError
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato que **toda** implementacion de los puertos de sincronizacion debe cumplir.
 *
 * Se ejecuta aqui contra las implementaciones en memoria, que son la referencia. Un consumidor que
 * escriba una version Mongo debe hacer pasar estas mismas aserciones: si diverge, la que esta mal
 * es la suya.
 *
 * Las invariantes que se fijan no son decorativas — cada una corresponde a un fallo real observado
 * en un consumidor antes de que estos puertos existieran.
 */
class SyncStoreContractTest {

    private fun tienda() = OId<Marketplace>()

    private fun listing(m: MarketplaceId, externalId: String, title: String = "t") =
        MarketplaceListing(marketplaceId = m, externalId = externalId, title = title)

    /* ------------------------------------------------------------------ IListingStore */

    /** Sin nada guardado se devuelve lista vacia, nunca `null`, y `count` concuerda. */
    @Test
    fun `un almacen vacio responde vacio`() = runBlocking {
        val store: IListingStore = InMemoryListingStore()
        val m = tienda()
        assertEquals(emptyList(), store.listings(m))
        assertEquals(0, store.count(m))
        assertNull(store.byExternalId(m, "no-existe"))
    }

    /**
     * Invariante: `upsert` es **idempotente** por (`marketplaceId`, `externalId`).
     *
     * El caché que esto reemplaza hacia `removeAll { it.externalId == … }; add(…)` a mano. Fijarlo
     * aqui evita que una implementacion acumule duplicados.
     */
    @Test
    fun `guardar dos veces el mismo listing deja uno y conserva el ultimo`() = runBlocking {
        val store = InMemoryListingStore()
        val m = tienda()
        store.upsert(listing(m, "SKU-1", "primero"))
        store.upsert(listing(m, "SKU-1", "segundo"))

        assertEquals(1, store.count(m))
        assertEquals("segundo", store.byExternalId(m, "SKU-1")?.title)
    }

    /**
     * Invariante: **los marketplaces no se mezclan**.
     *
     * Un `externalId` sólo es único dentro del suyo. Es el mismo error que ya se pagó en el mapeo de
     * ubicaciones de un consumidor: una clave global colapsando datos de tiendas distintas.
     */
    @Test
    fun `el mismo externalId en dos marketplaces son dos listings`() = runBlocking {
        val store = InMemoryListingStore()
        val a = tienda()
        val b = tienda()
        store.upsert(listing(a, "SKU-1", "de A"))
        store.upsert(listing(b, "SKU-1", "de B"))

        assertEquals(1, store.count(a))
        assertEquals(1, store.count(b))
        assertEquals("de A", store.byExternalId(a, "SKU-1")?.title)
        assertEquals("de B", store.byExternalId(b, "SKU-1")?.title)
    }

    /** `upsertAll` debe equivaler a N `upsert`, incluida la idempotencia. */
    @Test
    fun `upsertAll equivale a guardar uno por uno`() = runBlocking {
        val store = InMemoryListingStore()
        val m = tienda()
        store.upsertAll(listOf(listing(m, "A"), listing(m, "B"), listing(m, "A", "repetido")))

        assertEquals(2, store.count(m))
        assertEquals("repetido", store.byExternalId(m, "A")?.title)
    }

    /** El borrado es por elemento y no toca a los demas. */
    @Test
    fun `borrar un listing no arrastra a los otros`() = runBlocking {
        val store = InMemoryListingStore()
        val m = tienda()
        store.upsertAll(listOf(listing(m, "A"), listing(m, "B")))
        store.delete(m, "A")

        assertNull(store.byExternalId(m, "A"))
        assertEquals(1, store.count(m))
    }

    /* ------------------------------------------------------------------ ISyncRunStore */

    /** `last` devuelve la mas reciente por `finishedAt`, no la ultima insertada. */
    @Test
    fun `last devuelve la mas reciente aunque llegue desordenada`() = runBlocking {
        val store: ISyncRunStore = InMemorySyncRunStore()
        val m = tienda()
        store.record(SyncRun(m, SyncKind.RECONCILE, SyncOutcome.OK, finishedAt = 300))
        store.record(SyncRun(m, SyncKind.RECONCILE, SyncOutcome.FAILED, finishedAt = 100))

        assertEquals(300, store.last(m, SyncKind.RECONCILE)?.finishedAt)
    }

    /** El historial separa por tipo y por marketplace, y viene de nuevo a viejo. */
    @Test
    fun `el historial se filtra y ordena`() = runBlocking {
        val store = InMemorySyncRunStore()
        val m = tienda()
        val otra = tienda()
        store.record(SyncRun(m, SyncKind.RECONCILE, SyncOutcome.OK, finishedAt = 1))
        store.record(SyncRun(m, SyncKind.PUSH_INVENTORY, SyncOutcome.OK, finishedAt = 2))
        store.record(SyncRun(m, SyncKind.RECONCILE, SyncOutcome.OK, finishedAt = 3))
        store.record(SyncRun(otra, SyncKind.RECONCILE, SyncOutcome.OK, finishedAt = 9))

        assertEquals(listOf(3L, 1L), store.history(m, SyncKind.RECONCILE).map { it.finishedAt })
        assertEquals(3, store.history(m).size, "sin tipo, todas las de ese marketplace")
        assertEquals(listOf(3L), store.history(m, SyncKind.RECONCILE, limit = 1).map { it.finishedAt })
        assertNull(store.last(tienda(), SyncKind.RECONCILE), "un marketplace sin corridas no hereda las de otro")
    }

    /**
     * Invariante: **una omision sin motivo no se puede registrar**.
     *
     * Es el fallo silencioso convertido en regla del tipo. `pushInventoryToShopify` se saltaba el
     * trabajo con un `return` sin log durante meses; aqui `SKIPPED` sin `skippedReason` no compila
     * un objeto valido.
     */
    @Test
    fun `SKIPPED exige motivo`() = runBlocking {
        val m = tienda()
        assertFailsWith<IllegalArgumentException> {
            SyncRun(m, SyncKind.PUSH_INVENTORY, SyncOutcome.SKIPPED, finishedAt = 1)
        }
        val valida = SyncRun(
            m, SyncKind.PUSH_INVENTORY, SyncOutcome.SKIPPED, finishedAt = 1,
            skippedReason = "almacenes sin mapear: 9, 12",
        )
        assertTrue(valida.skippedReason!!.isNotBlank())
    }

    /** Invariante: `OK` con errores es una contradiccion; ese caso es `PARTIAL`. */
    @Test
    fun `OK no admite errores`() = runBlocking {
        val m = tienda()
        assertFailsWith<IllegalArgumentException> {
            SyncRun(
                m, SyncKind.PUSH_PRODUCTS, SyncOutcome.OK, finishedAt = 1,
                errors = listOf(SyncRunError(item = "SKU-1", message = "falló")),
            )
        }
        val parcial = SyncRun(
            m, SyncKind.PUSH_PRODUCTS, SyncOutcome.PARTIAL, finishedAt = 1, changed = 4,
            errors = listOf(SyncRunError(item = "SKU-1", message = "falló")),
        )
        assertEquals(1, parcial.errors.size)
    }

    /** Los errores guardan **texto**, no sólo cuántos: un contador sin motivo no es observabilidad. */
    @Test
    fun `el historial conserva el mensaje de cada error`() = runBlocking {
        val store = InMemorySyncRunStore()
        val m = tienda()
        store.record(
            SyncRun(
                m, SyncKind.RECONCILE, SyncOutcome.FAILED, finishedAt = 1,
                errors = listOf(SyncRunError(message = "ERP read failed: campo 'published' ausente")),
            )
        )
        val recuperado = store.last(m, SyncKind.RECONCILE)!!
        assertTrue(
            recuperado.errors.single().message.contains("published"),
            "el motivo debe sobrevivir al almacenamiento; es lo que faltaba cuando sólo se guardaba el conteo"
        )
    }

    /* -------------------------------------------------------------- ISyncCheckpointStore */

    /** Ausente significa «desde el principio», y se distingue de una cadena vacia guardada. */
    @Test
    fun `un checkpoint ausente es null y no cadena vacia`() = runBlocking {
        val store: ISyncCheckpointStore = InMemorySyncCheckpointStore()
        val m = tienda()
        assertNull(store.get(m, "products.cursor"))

        store.set(m, "products.cursor", "")
        assertEquals("", store.get(m, "products.cursor"), "una cadena vacía guardada NO es lo mismo que ausente")
    }

    /** Se reemplaza, se limpia, y no se mezcla entre marketplaces ni entre claves. */
    @Test
    fun `los checkpoints se aislan por marketplace y por clave`() = runBlocking {
        val store = InMemorySyncCheckpointStore()
        val a = tienda()
        val b = tienda()
        store.set(a, "products.cursor", "a1")
        store.set(a, "collections.cursor", "a2")
        store.set(b, "products.cursor", "b1")

        assertEquals("a1", store.get(a, "products.cursor"))
        assertEquals("a2", store.get(a, "collections.cursor"))
        assertEquals("b1", store.get(b, "products.cursor"))

        store.set(a, "products.cursor", "a1-nuevo")
        assertEquals("a1-nuevo", store.get(a, "products.cursor"))

        store.clear(a, "products.cursor")
        assertNull(store.get(a, "products.cursor"), "limpiar fuerza la pasada completa")
        assertEquals("a2", store.get(a, "collections.cursor"), "limpiar una clave no toca las demás")
        assertEquals("b1", store.get(b, "products.cursor"), "ni las de otro marketplace")
    }
}
