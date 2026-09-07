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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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

    /**
     * Invariante: **reemplazar conserva el `_id` local**.
     *
     * `CostBreakdown.listingId` y `MarketplaceListingDraft.publishedListingId` apuntan al `_id` local
     * del listing. Una conciliacion reconstruye cada listing desde el remoto con un objeto nuevo, y un
     * objeto nuevo trae un `_id` nuevo. Si el almacen lo adoptara, cada conciliacion dejaria esas
     * referencias apuntando a un identificador que ya no existe, sin que nada fallara.
     */
    @Test
    fun `reemplazar un listing conserva su _id local`() = runBlocking {
        val store = InMemoryListingStore()
        val m = tienda()
        val original = listing(m, "SKU-1", "primero")
        store.upsert(original)
        val reconstruido = listing(m, "SKU-1", "segundo")
        assertNotEquals(original._id, reconstruido._id, "el objeto nuevo trae un _id nuevo: es la premisa de la prueba")
        store.upsert(reconstruido)

        val guardado = store.byExternalId(m, "SKU-1")!!
        assertEquals(original._id, guardado._id, "la identidad local la fija la primera escritura")
        assertEquals("segundo", guardado.title, "los datos sí son los del que llega")
        assertEquals(1, store.count(m))
    }

    /** La conservacion del `_id` vale tambien en lote; tras un borrado explicito la identidad empieza de cero. */
    @Test
    fun `upsertAll conserva el _id y borrar lo libera`() = runBlocking {
        val store = InMemoryListingStore()
        val m = tienda()
        val original = listing(m, "A")
        store.upsert(original)
        store.upsertAll(listOf(listing(m, "A", "otra vez"), listing(m, "B")))
        assertEquals(original._id, store.byExternalId(m, "A")?._id, "en lote se conserva igual")
        assertEquals("otra vez", store.byExternalId(m, "A")?.title)

        store.delete(m, "A")
        val renacido = listing(m, "A", "renacido")
        store.upsert(renacido)
        assertEquals(renacido._id, store.byExternalId(m, "A")?._id, "borrar es explícito: después la clave está libre")
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

    /**
     * Invariante: **una corrida es una instantanea**.
     *
     * «OK sin errores» se comprueba al construir. Si la corrida guardara la lista del llamador,
     * bastaria seguir añadiendo a esa lista para que una corrida registrada como `OK` apareciera con
     * errores al leerla del historial — sin `record` de por medio y sin que la comprobacion volviera
     * a correr.
     */
    @Test
    fun `mutar la lista de errores despues de registrar no cambia la corrida`() = runBlocking {
        val store = InMemorySyncRunStore()
        val m = tienda()
        val errores = mutableListOf<SyncRunError>()
        val corrida = SyncRun(m, SyncKind.RECONCILE, SyncOutcome.OK, finishedAt = 1, errors = errores)
        store.record(corrida)

        errores += SyncRunError(item = "SKU-1", message = "llegó después de registrar")

        assertTrue(corrida.errors.isEmpty(), "la corrida no comparte la lista con quien la construyó")
        assertTrue(store.last(m, SyncKind.RECONCILE)!!.errors.isEmpty(), "ni el historial")
        assertEquals(SyncOutcome.OK, store.last(m, SyncKind.RECONCILE)!!.outcome)
    }

    /**
     * La deserializacion entra por el constructor privado: el serializador generado lo alcanza, las
     * invariantes de `init` corren igual, y la corrida decodificada es una instantanea propia.
     */
    @Test
    fun `una corrida sobrevive a la serializacion`() = runBlocking {
        val m = tienda()
        val corrida = SyncRun(
            m, SyncKind.PUSH_PRODUCTS, SyncOutcome.PARTIAL, finishedAt = 7, changed = 2,
            errors = listOf(SyncRunError(item = "SKU-1", message = "falló")),
        )
        val decodificada = Json.decodeFromString<SyncRun>(Json.encodeToString(corrida))

        assertEquals(corrida, decodificada)
        val rechazo = assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString<SyncRun>(Json.encodeToString(corrida).replace("PARTIAL", "OK"))
        }
        assertTrue(
            rechazo.message!!.contains("OK no admite errores"),
            "las invariantes también se comprueban al decodificar"
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
