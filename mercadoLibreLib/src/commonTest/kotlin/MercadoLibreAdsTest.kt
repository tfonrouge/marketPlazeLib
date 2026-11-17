import com.fonrouge.base.types.OId
import kotlin.test.assertFailsWith
import com.fonrouge.base.types.StringId
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsPeriod
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsEvidence
import com.fonrouge.commonLib.model.Money
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsAssembly
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayRead
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayStatus
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsCycle
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayReader
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsRequest
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsResponse
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsIdentity
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsReadAction
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSavedState
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSource
import com.fonrouge.mercadoLibreLib.model.applyAdsRead
import com.fonrouge.mercadoLibreLib.model.assembleAdsDay
import com.fonrouge.mercadoLibreLib.model.planAdsReads
import com.fonrouge.mercadoLibreLib.model.runAdsCycle
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Casos de la especificación ejecutable de PublicidadMercadoAds (`especificacion/pruebas.py`, A1–A12 y P1–P3), con las
 * mismas respuestas reales del 6-sep, más el plan de lectura y el interruptor (PL1–PL7) y el cupo medido en solicitudes
 * (Q1–Q11), que la especificación no cubre.
 */
class MercadoLibreAdsTest {

    private val identity = MercadoLibreAdsIdentity(OId("69c6d0497a19acc9db44ba89"), MercadoLibreAdsProduct.PADS, 2303516, "MLM", "MXN")
    private fun obj(text: String) = Json.parseToJsonElement(text).jsonObject

    private data class Entradas(val pages: List<JsonObject>, val daily: JsonObject, val ads: Map<Long, JsonObject>)

    private fun entradas() = Entradas(
        listOf(obj(MercadoLibreAdsFixturesSep06.adGroupsOffset0), obj(MercadoLibreAdsFixturesSep06.adGroupsOffset50)),
        obj(MercadoLibreAdsFixturesSep06.campaignsDaily),
        mapOf(
            2260824327L to obj(MercadoLibreAdsFixturesSep06.ads2260824327),
            2660965675L to obj(MercadoLibreAdsFixturesSep06.ads2660965675),
            2260754914L to obj(MercadoLibreAdsFixturesSep06.ads2260754914),
            3023399752L to obj(MercadoLibreAdsFixturesSep06.ads3023399752),
        ),
    )

    private fun armar(e: Entradas, source: MercadoLibreAdsSource = MercadoLibreAdsSource.API) =
        assembleAdsDay("2026-09-06", e.pages, e.daily, e.ads, source)

    private fun ok(e: Entradas, source: MercadoLibreAdsSource = MercadoLibreAdsSource.API): MercadoLibreAdsDayRead =
        when (val a = armar(e, source)) {
            is MercadoLibreAdsAssembly.Ok -> a.read
            is MercadoLibreAdsAssembly.Rejected -> fail("rechazado: ${a.reason}")
        }

    private fun rechaza(e: Entradas, texto: String) {
        val a = armar(e)
        assertIs<MercadoLibreAdsAssembly.Rejected>(a, "debía rechazar ($texto)")
        assertTrue(texto in a.reason, "rechazó por otra razón: ${a.reason}")
    }

    /** Cambia `metrics.cost` del resultado que cumpla [cual]. */
    private fun JsonObject.conCosto(cual: (JsonObject) -> Boolean, costo: Double): JsonObject {
        val results = (this["results"] as JsonArray).map { r ->
            val o = r.jsonObject
            if (!cual(o)) o else JsonObject(o + ("metrics" to JsonObject((o["metrics"] as JsonObject) + ("cost" to JsonPrimitive(costo)))))
        }
        return JsonObject(this + ("results" to JsonArray(results)))
    }

    private fun JsonObject.conCostoDiario(costo: Double): JsonObject =
        JsonObject(this + ("results" to JsonArray((this["results"] as JsonArray).map { JsonObject(it.jsonObject + ("cost" to JsonPrimitive(costo))) })))

    private fun id(o: JsonObject) = (o["id"] as? JsonPrimitive)?.longOrNull

    // ------------------------------------------------------------------ captura

    @Test
    fun a1_63AdGroupsEnDosPaginasCuadran206_57() {
        val d = ok(entradas())
        assertEquals(63, d.adGroupsTotal)
        assertEquals(Money(20_657), d.total)
        assertEquals(
            mapOf("MLM5072161188" to 4_800L, "MLM3219448913" to 1_257L, "MLM2801137851" to 0L, "MLM2806404171" to 14_600L),
            d.rows.associate { it.itemId!! to it.cost.cents },
        )
    }

    @Test
    fun a2_respuestaParcialSinPagina2SeRechaza() {
        val e = entradas()
        rechaza(e.copy(pages = e.pages.take(1)), "paginación incompleta")
    }

    @Test
    fun a3_paginaFueraDeOrdenSeRechaza() {
        val e = entradas()
        rechaza(e.copy(pages = e.pages.reversed()), "paginación")
    }

    @Test
    fun a4_faltaElDesgloseDeUnAdGroupConCosto() {
        val e = entradas()
        rechaza(e.copy(ads = e.ads - 2260754914L), "falta el desglose")
    }

    @Test
    fun a5_c2ElDesgloseNoSumaElCostoDelAdGroup() {
        val e = entradas()
        val ads = e.ads + (2660965675L to e.ads.getValue(2660965675L).conCosto({ (it["item_id"] as JsonPrimitive).content == "MLM3219448913" }, 12.00))
        rechaza(e.copy(ads = ads), "C-2")
    }

    @Test
    fun a6_c1ElTotalDiarioNoCuadra() {
        val e = entradas()
        rechaza(e.copy(daily = e.daily.conCostoDiario(200.00)), "C-1")
    }

    @Test
    fun a7_c3FilaRepetida() {
        val e = entradas()
        val cubeta = e.ads.getValue(2260824327L)
        val repetida = JsonObject((cubeta["results"] as JsonArray)[0].jsonObject + ("metrics" to JsonObject(mapOf("cost" to JsonPrimitive(0)))))
        val conRepetida = JsonObject(cubeta + ("results" to JsonArray((cubeta["results"] as JsonArray) + repetida)))
        rechaza(e.copy(ads = e.ads + (2260824327L to conRepetida)), "C-3")
    }

    @Test
    fun a8_grupoCompartidoConservaElCeroMedido() {
        val filas = ok(entradas()).rows.filter { it.adGroupId == 2660965675L }
        assertEquals(mapOf("MLM3219448913" to 1_257L, "MLM2801137851" to 0L), filas.associate { it.itemId!! to it.cost.cents })
        assertTrue(filas.all { it.adGroupType == "CATALOG" })
    }

    @Test
    fun a9_laPublicacionNuncaEsElAdGroupExternalId() {
        assertTrue("MLM76039477" !in ok(entradas()).rows.map { it.itemId })
    }

    @Test
    fun a10_relecturaIgualNoCambiaLaRevisionYDistintaGuardaLaAnterior() {
        val e = entradas()
        val c = applyAdsRead(null, ok(e), "t1", identity)
        val c2 = applyAdsRead(c.day, ok(e), "t2", identity)
        assertEquals(listOf(MercadoLibreAdsReadAction.CREATED, MercadoLibreAdsReadAction.UNCHANGED), listOf(c.action, c2.action))
        assertEquals(1, c2.day.revision)
        assertEquals("t2", c2.day.lastReadAt)
        val cambiado = e.copy(
            pages = listOf(e.pages[0], e.pages[1].conCosto({ id(it) == 2260754914L }, 140.00)),
            daily = e.daily.conCostoDiario(200.57),
            ads = e.ads + (2260754914L to e.ads.getValue(2260754914L).conCosto({ true }, 140.00)),
        )
        val c3 = applyAdsRead(c2.day, ok(cambiado), "t3", identity)
        assertEquals(MercadoLibreAdsReadAction.REVISED, c3.action)
        assertEquals(2, c3.day.revision)
        assertEquals(Money(20_057), c3.day.total)
        assertEquals(Money(20_657), c3.day.history.single().total)
        assertEquals("t1", c3.day.firstReadAt)
    }

    @Test
    fun a11_cortes401DetieneElCicloY500SaltaElDia() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val api = ApiFalsa(estadoPara = { r ->
            when {
                r.date == "2026-09-07" && r is MercadoLibreAdsRequest.CampaignsDaily -> 500
                r.date == "2026-09-08" -> 401
                else -> 200
            }
        })
        val r = ciclo(listOf("2026-09-06", "2026-09-07", "2026-09-08", "2026-09-09"), 500, api, guardados)
        assertEquals(setOf("2026-09-06"), guardados.keys)
        assertEquals(listOf("2026-09-07"), r.retryable)
        assertEquals("2026-09-08" to 401, r.cut)
        assertEquals(6 + 3 + 1, r.requests) // día completo + 2 páginas y el 500 + el 401
        assertEquals(1, r.notStarted)
        assertEquals(r.requests, api.pedidas.size)
    }

    @Test
    fun a12_unRechazoNoBorraLoGuardado() {
        val g = applyAdsRead(null, ok(entradas()), "t1", identity).day
        val guardados = mutableMapOf(g.date to g)
        val api = ApiFalsa(cuerpoPara = { r -> if (r is MercadoLibreAdsRequest.CampaignsDaily) obj(F.campaignsDaily).conCostoDiario(1.0) else null })
        val r = ciclo(listOf(g.date), 500, api, guardados, "t2")
        assertSame(g, guardados[g.date])
        assertTrue(r.rejected.getValue(g.date).startsWith("C-1"))
    }

    // ------------------------------------------------------------------ precedencia

    @Test
    fun p1_laApiReemplazaAlPanelConElMismoValorYElPanelQuedaEnElHistorial() {
        val e = entradas()
        val panel = applyAdsRead(null, ok(e, MercadoLibreAdsSource.PANEL_DIARIO), "t1", identity).day
        val api = applyAdsRead(panel, ok(e), "t2", identity)
        assertEquals(MercadoLibreAdsReadAction.SOURCE_CHANGED, api.action)
        assertEquals(MercadoLibreAdsSource.API, api.day.source)
        assertEquals(listOf(MercadoLibreAdsSource.PANEL_DIARIO), api.day.history.map { it.source })
    }

    @Test
    fun p2_elPanelNuncaSobrescribeALaApi() {
        val e = entradas()
        val api = applyAdsRead(null, ok(e), "t1", identity).day
        val r = applyAdsRead(api, ok(e, MercadoLibreAdsSource.PANEL_DIARIO), "t2", identity)
        assertEquals(MercadoLibreAdsReadAction.IGNORED_LOWER_SOURCE, r.action)
        assertSame(api, r.day)
    }

    @Test
    fun p3_apiDistintaDelPanelQuedaMarcada() {
        val e = entradas()
        val panel = applyAdsRead(null, ok(e, MercadoLibreAdsSource.PANEL_DIARIO), "t1", identity).day
        val distinta = e.copy(
            pages = listOf(e.pages[0].conCosto({ id(it) == 2260824327L }, 47.00), e.pages[1]),
            daily = e.daily.conCostoDiario(205.57),
            ads = e.ads + (2260824327L to e.ads.getValue(2260824327L).conCosto({ true }, 47.00)),
        )
        val r = applyAdsRead(panel, ok(distinta), "t2", identity)
        assertEquals(MercadoLibreAdsReadAction.SOURCE_CHANGED_WITH_DISCREPANCY, r.action)
        assertEquals(MercadoLibreAdsSource.PANEL_DIARIO, r.day.history.single().source)
    }

    // ------------------------------------------------------------------ plan e interruptor (no están en la especificación)

    private val hoy = LocalDate(2026, 11, 12)
    private val guardados = mapOf(
        "2026-08-14" to MercadoLibreAdsSavedState(MercadoLibreAdsDayStatus.PROVISIONAL, MercadoLibreAdsSource.PANEL_DIARIO, "2026-11-12"),
        "2026-08-15" to MercadoLibreAdsSavedState(MercadoLibreAdsDayStatus.PROVISIONAL, MercadoLibreAdsSource.API, "2026-11-11"),
        "2026-11-05" to MercadoLibreAdsSavedState(MercadoLibreAdsDayStatus.PROVISIONAL, MercadoLibreAdsSource.API, "2026-11-11"),
        "2026-11-06" to MercadoLibreAdsSavedState(MercadoLibreAdsDayStatus.PROVISIONAL, MercadoLibreAdsSource.API, "2026-11-12"),
        "2026-09-01" to MercadoLibreAdsSavedState(MercadoLibreAdsDayStatus.CERRADO, MercadoLibreAdsSource.API, "2026-09-20"),
    )

    @Test
    fun pl1_conElInterruptorApagadoHayCeroLlamadas() {
        val plan = planAdsReads(hoy, 9, enabledFrom = null, saved = guardados, rereadDays = 14)
        assertEquals(emptyList(), plan.reads)
        assertEquals(emptyList(), plan.close)
        assertEquals("interruptor apagado", plan.skippedBy)
        val api = ApiFalsa()
        val r = runAdsCycle(MercadoLibreAdsCycle(plan.reads, 60), "t", identity, get = api::responder,
            load = { fail("no debía leer") }, store = { _, _ -> fail("no debía escribir") })
        assertEquals(0, api.pedidas.size)
        assertEquals(0, r.requests)
    }

    @Test
    fun pl2_ordenFaltantesDelMasViejoDespuesRelecturaDespuesPanelAApi() {
        val plan = planAdsReads(hoy, 9, "2026-11-12", guardados, rereadDays = 14)
        assertEquals("2026-08-16", plan.reads.first()) // 14 y 15 ya tienen documento
        val i = plan.reads.indexOf("2026-11-05")
        assertTrue(i > plan.reads.indexOf("2026-11-11"), "la relectura va después de todos los faltantes")
        assertEquals("2026-08-14", plan.reads.last()) // el día del panel pasa a API al final
        assertTrue("2026-09-01" !in plan.reads, "un día CERRADO no se relee solo")
    }

    @Test
    fun pl3_ayerSoloDesdeLas7DeCiudadDeMexico() {
        assertTrue("2026-11-11" in planAdsReads(hoy, 7, "x", emptyMap(), rereadDays = 14).reads)
        assertTrue("2026-11-11" !in planAdsReads(hoy, 6, "x", emptyMap(), rereadDays = 14).reads)
    }

    @Test
    fun pl4_elPlanNoRecortaYElCupoEnSolicitudesSeAplicaDespuesDeOrdenar() {
        val plan = planAdsReads(hoy, 9, "x", guardados, rereadDays = 14)
        assertEquals(85 + 1 + 1, plan.reads.size) // 90 días del 14-ago al 11-nov: 85 faltantes, 1 relectura y 1 del panel
        val guardadosCiclo = mutableMapOf<String, MercadoLibreAdsDay>()
        val r = ciclo(plan.reads, 60, ApiFalsa(), guardadosCiclo)
        assertEquals(plan.reads.take(10), guardadosCiclo.keys.sorted()) // 10 días × 6 solicitudes = 60
        assertEquals(60, r.requests)
    }

    @Test
    fun pl5_unDiaProvisionalDeLaApiQueCumpleLaVentanaSeCierraSinLeer() {
        val plan = planAdsReads(hoy, 9, "x", guardados, rereadDays = 14)
        assertEquals(listOf("2026-08-15"), plan.close)
        assertTrue("2026-08-15" !in plan.reads)
    }

    @Test
    fun pl6_unDiaYaReleidoHoyNoSeRelee() {
        assertTrue("2026-11-06" !in planAdsReads(hoy, 9, "x", guardados, rereadDays = 14).reads)
    }

    @Test
    fun pl7_laVentanaEmpiezaEnHoyMenos90() {
        val plan = planAdsReads(hoy, 9, "x", emptyMap(), rereadDays = 14)
        assertEquals("2026-08-14", plan.reads.first())
        assertEquals(90, plan.reads.size) // del 14-ago al 11-nov
    }

    // ------------------------------------------------------------------ cupo medido en solicitudes (B2 S4, L-025)

    /** Respuestas reales del 6-sep servidas para cualquier fecha; sólo el total diario lleva la fecha pedida. */
    private val F = MercadoLibreAdsFixturesSep06

    private fun real(r: MercadoLibreAdsRequest): JsonObject = when (r) {
        is MercadoLibreAdsRequest.AdGroupsPage -> when (r.offset) {
            0 -> obj(F.adGroupsOffset0)
            50 -> obj(F.adGroupsOffset50)
            else -> fail("offset inesperado ${r.offset}")
        }
        is MercadoLibreAdsRequest.CampaignsDaily -> obj(F.campaignsDaily.replace("2026-09-06", r.date))
        is MercadoLibreAdsRequest.AdGroupAds -> obj(
            when (r.adGroupId) {
                2260824327L -> F.ads2260824327
                2660965675L -> F.ads2660965675
                2260754914L -> F.ads2260754914
                3023399752L -> F.ads3023399752
                else -> fail("ad group inesperado ${r.adGroupId}")
            }
        )
    }

    /** Una API falsa que registra cada solicitud. [estadoPara] decide el estado HTTP; [cuerpoPara] puede cambiar el cuerpo. */
    private inner class ApiFalsa(
        val estadoPara: (MercadoLibreAdsRequest) -> Int = { 200 },
        val cuerpoPara: (MercadoLibreAdsRequest) -> JsonObject? = { null },
    ) {
        val pedidas = mutableListOf<MercadoLibreAdsRequest>()
        fun responder(r: MercadoLibreAdsRequest): MercadoLibreAdsResponse {
            pedidas += r
            return when (val estado = estadoPara(r)) {
                200 -> MercadoLibreAdsResponse.Ok(cuerpoPara(r) ?: real(r))
                in 500..599 -> MercadoLibreAdsResponse.Retryable("estado $estado")
                else -> MercadoLibreAdsResponse.Cut(estado, "estado $estado")
            }
        }
    }

    private fun ciclo(
        fechas: List<String>, cupo: Int, api: ApiFalsa, guardados: MutableMap<String, MercadoLibreAdsDay>, ahora: String = "t",
    ) = runAdsCycle(MercadoLibreAdsCycle(fechas, cupo), ahora, identity, get = api::responder,
        load = { guardados[it] }, store = { d, previo -> guardarSi(guardados, d, previo) })

    @Test
    fun q1_unDiaRealCuesta6SolicitudesEnEsteOrden() {
        val lector = MercadoLibreAdsDayReader("2026-09-06")
        val pedidas = mutableListOf<MercadoLibreAdsRequest>()
        while (true) {
            val r = lector.next() ?: break
            pedidas += r
            lector.accept(real(r))
        }
        assertEquals(
            listOf(
                MercadoLibreAdsRequest.AdGroupsPage("2026-09-06", 0),
                MercadoLibreAdsRequest.AdGroupsPage("2026-09-06", 50),
                MercadoLibreAdsRequest.CampaignsDaily("2026-09-06"),
                MercadoLibreAdsRequest.AdGroupAds("2026-09-06", 2660965675L, 0),
                MercadoLibreAdsRequest.AdGroupAds("2026-09-06", 2260824327L, 0),
                MercadoLibreAdsRequest.AdGroupAds("2026-09-06", 2260754914L, 0),
            ),
            pedidas,
        )
        val a = lector.assemble()
        assertIs<MercadoLibreAdsAssembly.Ok>(a)
        assertEquals(ok(entradas()), a.read) // lo mismo que armar con las respuestas de la sonda
    }

    @Test
    fun q2_unaPaginacionQueNoCuadraNoGastaMasCupo() {
        val lector = MercadoLibreAdsDayReader("2026-09-06")
        val pedidas = mutableListOf<MercadoLibreAdsRequest>()
        while (true) {
            val r = lector.next() ?: break
            pedidas += r
            // la página 2 llega vacía: la paginación queda en 50 de 63
            lector.accept(if (r == MercadoLibreAdsRequest.AdGroupsPage("2026-09-06", 50)) obj("""{"paging":{"offset":50,"total":63},"results":[]}""") else real(r))
        }
        assertEquals(2, pedidas.size) // ni total diario ni desglose
        val a = lector.assemble()
        assertIs<MercadoLibreAdsAssembly.Rejected>(a)
        assertTrue("paginación incompleta" in a.reason, a.reason)
    }

    @Test
    fun q3_elDesglosePaginadoSePideCompletoYCuentaCadaPagina() {
        // AUREVO-01: el ad group de catálogo trae 2 publicaciones; ML las da en dos páginas de 1.
        val completo = obj(F.ads2660965675)
        val anuncios = completo["results"] as JsonArray
        fun pagina(offset: Int) = JsonObject(mapOf(
            "paging" to obj("""{"offset":$offset,"total":2,"limit":1}"""),
            "results" to JsonArray(listOf(anuncios[offset])),
        ))
        val lector = MercadoLibreAdsDayReader("2026-09-06")
        var solicitudes = 0
        while (true) {
            val r = lector.next() ?: break
            solicitudes++
            lector.accept(if (r is MercadoLibreAdsRequest.AdGroupAds && r.adGroupId == 2660965675L) pagina(r.offset) else real(r))
        }
        assertEquals(7, solicitudes) // una más que el día real
        assertEquals(ok(entradas()), (lector.assemble() as MercadoLibreAdsAssembly.Ok).read)
    }

    @Test
    fun q4_elCupoNuncaSeRebasaYUnDiaAMediasNoSeEscribe() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val api = ApiFalsa()
        val r = ciclo(listOf("2026-09-06", "2026-09-07", "2026-09-08", "2026-09-09"), 14, api, guardados)
        assertEquals(14, api.pedidas.size) // 6 + 6 + 2: el cupo cuenta páginas y desglose
        assertEquals(14, r.requests)
        assertEquals(setOf("2026-09-06", "2026-09-07"), guardados.keys)
        assertEquals("2026-09-08", r.quotaExhaustedAt)
        assertEquals(null, r.exceedsQuota)
        assertEquals(1, r.notStarted)
        assertEquals(2, api.pedidas.count { it is MercadoLibreAdsRequest.AdGroupsPage && it.date == "2026-09-08" })
    }

    @Test
    fun q5_elSiguienteCicloRetomaElDiaQueQuedoAMedias() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val fechas = listOf("2026-09-06", "2026-09-07", "2026-09-08")
        val r1 = ciclo(fechas, 8, ApiFalsa(), guardados, "t1")
        assertEquals("2026-09-07", r1.quotaExhaustedAt)
        // el plan del siguiente ciclo: lo que falta, en el mismo orden
        val faltan = fechas.filter { it !in guardados }
        assertEquals(listOf("2026-09-07", "2026-09-08"), faltan)
        val r2 = ciclo(faltan, 60, ApiFalsa(), guardados, "t2")
        assertEquals(null, r2.quotaExhaustedAt)
        assertEquals(12, r2.requests)
        assertEquals(fechas.toSet(), guardados.keys)
        assertTrue(guardados.values.all { it.total == Money(20_657) && it.revision == 1 })
    }

    @Test
    fun q6_un429ALaMitadCortaSinEscribirYElSiguienteCicloRetoma() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val api = ApiFalsa(estadoPara = { r -> if (r == MercadoLibreAdsRequest.AdGroupsPage("2026-09-07", 50)) 429 else 200 })
        val r1 = ciclo(listOf("2026-09-06", "2026-09-07", "2026-09-08"), 60, api, guardados, "t1")
        assertEquals("2026-09-07" to 429, r1.cut)
        assertEquals(8, api.pedidas.size) // nada después del 429
        assertEquals(api.pedidas.last(), MercadoLibreAdsRequest.AdGroupsPage("2026-09-07", 50))
        assertEquals(setOf("2026-09-06"), guardados.keys)
        assertEquals(1, r1.notStarted)
        val r2 = ciclo(listOf("2026-09-07", "2026-09-08"), 60, ApiFalsa(), guardados, "t2")
        assertEquals(null, r2.cut)
        assertEquals(setOf("2026-09-06", "2026-09-07", "2026-09-08"), guardados.keys)
    }

    @Test
    fun q7_unDiaParcialConservaLaUltimaRevisionValida() {
        val e = entradas()
        val previo = applyAdsRead(null, ok(e), "t1", identity).day
        val revisado = applyAdsRead(previo, ok(e.copy(
            pages = listOf(e.pages[0].conCosto({ id(it) == 2260824327L }, 47.00), e.pages[1]),
            daily = e.daily.conCostoDiario(205.57),
            ads = e.ads + (2260824327L to e.ads.getValue(2260824327L).conCosto({ true }, 47.00)),
        )), "t2", identity).day
        assertEquals(2, revisado.revision)
        val panel = applyAdsRead(null, ok(e, MercadoLibreAdsSource.PANEL_DIARIO), "t1", identity).day.copy(date = "2026-09-07")
        // relectura del 06 (revisión 2) y paso a API del 07 (panel), cortados a la mitad de tres maneras
        val cortes = listOf<(MercadoLibreAdsRequest) -> Int>(
            { r -> if (r is MercadoLibreAdsRequest.AdGroupAds) 429 else 200 },
            { r -> if (r is MercadoLibreAdsRequest.CampaignsDaily) 503 else 200 },
        )
        for (corte in cortes) {
            val guardados = mutableMapOf(revisado.date to revisado, panel.date to panel)
            ciclo(listOf("2026-09-06", "2026-09-07"), 60, ApiFalsa(estadoPara = corte), guardados, "t3")
            assertSame(revisado, guardados["2026-09-06"])
            assertSame(panel, guardados["2026-09-07"])
        }
        val guardados = mutableMapOf(revisado.date to revisado, panel.date to panel)
        val r = ciclo(listOf("2026-09-06", "2026-09-07"), 4, ApiFalsa(), guardados, "t3") // el cupo se acaba a media relectura
        assertEquals("2026-09-06", r.quotaExhaustedAt)
        assertSame(revisado, guardados["2026-09-06"])
        assertSame(panel, guardados["2026-09-07"])
        assertEquals(2, guardados.getValue("2026-09-06").revision)
        assertEquals(Money(20_557), guardados.getValue("2026-09-06").total)
    }

    @Test
    fun q8_unDiaQueNoCabeEnElCupoCompletoSeAvisa() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val api = ApiFalsa()
        val r = ciclo(listOf("2026-09-06", "2026-09-07"), 5, api, guardados)
        assertEquals("2026-09-06", r.exceedsQuota)
        assertEquals("2026-09-06", r.quotaExhaustedAt)
        assertEquals(5, api.pedidas.size)
        assertTrue(guardados.isEmpty())
    }

    @Test
    fun q9_noEmpiezaUnDiaConMenosDeDosSolicitudesDisponibles() {
        val api = ApiFalsa()
        val r = ciclo(listOf("2026-09-06", "2026-09-07"), 7, api, mutableMapOf())
        assertEquals(6, api.pedidas.size) // el séptimo no se gasta en un día que no puede terminar
        assertEquals(null, r.quotaExhaustedAt)
        assertEquals(1, r.notStarted)
        val cero = ApiFalsa()
        assertEquals(0, ciclo(listOf("2026-09-06"), 0, cero, mutableMapOf()).requests)
        assertEquals(0, cero.pedidas.size)
    }

    @Test
    fun q10_un500ALaMitadSaltaElDiaYElCicloSigue() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        val api = ApiFalsa(estadoPara = { r -> if (r.date == "2026-09-06" && r is MercadoLibreAdsRequest.AdGroupAds) 500 else 200 })
        val r = ciclo(listOf("2026-09-06", "2026-09-07"), 60, api, guardados)
        assertEquals(listOf("2026-09-06"), r.retryable)
        assertEquals(setOf("2026-09-07"), guardados.keys)
        assertEquals(4 + 6, r.requests)
    }

    @Test
    fun q11_productoNoHabilitadoAMitadDeUnDiaCortaComoUn404() {
        val guardados = mutableMapOf<String, MercadoLibreAdsDay>()
        var pedidas = 0
        val r = runAdsCycle(MercadoLibreAdsCycle(listOf("2026-09-06", "2026-09-07"), 60), "t", identity,
            get = { pedidas++; MercadoLibreAdsResponse.NotEnabled }, load = { guardados[it] }, store = { d, previo -> guardarSi(guardados, d, previo) })
        assertEquals("2026-09-06" to 404, r.cut)
        assertEquals(1, pedidas)
        assertEquals(1, r.notStarted)
        assertTrue(guardados.isEmpty())
    }

    /** Escritura condicionada en memoria, con la misma regla que Mongo: misma revisión y misma última lectura. */
    private fun guardarSi(guardados: MutableMap<String, MercadoLibreAdsDay>, d: MercadoLibreAdsDay, previo: MercadoLibreAdsDay?): Boolean {
        val actual = guardados[d.date]
        val vigente = if (previo == null) actual == null else actual != null && actual.revision == previo.revision && actual.lastReadAt == previo.lastReadAt
        if (vigente) guardados[d.date] = d
        return vigente
    }

    @Test
    fun q12_siOtroEscritorCambioElDiaNoSeSobrescribeYQuedaComoConflicto() {
        val e = entradas()
        val panel = applyAdsRead(null, ok(e, MercadoLibreAdsSource.PANEL_DIARIO), "t1", identity).day
        val guardados = mutableMapOf(panel.date to panel)
        // Entre la lectura del ciclo y su escritura, una relectura manual deja el día en otra revisión.
        val otro = applyAdsRead(panel, ok(e), "t-manual", identity).day
        val r = runAdsCycle(MercadoLibreAdsCycle(listOf("2026-09-06", "2026-09-07"), 60), "t2", identity, get = ApiFalsa()::responder,
            load = { guardados[it] },
            store = { d, previo ->
                if (d.date == "2026-09-06") guardados[d.date] = otro
                guardarSi(guardados, d, previo)
            })
        assertEquals(listOf("2026-09-06"), r.conflicts)
        assertSame(otro, guardados["2026-09-06"])
        assertTrue("2026-09-06" !in r.actions)
        assertEquals(MercadoLibreAdsReadAction.CREATED, r.actions["2026-09-07"])
    }

    // ------------------------------------------------------------------ periodos (B1 R20, R21)

    private fun periodo(desde: String, hasta: String, evidencia: List<MercadoLibreAdsEvidence>) = MercadoLibreAdsPeriod(
        _id = StringId(MercadoLibreAdsPeriod.idOf(identity, desde, hasta)), marketplaceId = identity.marketplaceId,
        product = MercadoLibreAdsProduct.PADS, advertiserId = identity.advertiserId, siteId = "MLM", currencyId = "MXN",
        from = desde, to = hasta, granularity = "periodo", rows = emptyList(), total = Money(2_000),
        source = MercadoLibreAdsSource.PANEL_PERIODO, evidence = evidencia, importedAt = "t",
    )

    private val h1 = "a".repeat(64)
    private val h2 = "0123456789abcdef".repeat(4)

    @Test
    fun pe1_unPeriodoGuardaVariosReportesDeOrigenSinUnirlos() {
        val p = periodo("2026-08-01", "2026-08-05", listOf(MercadoLibreAdsEvidence("r6.csv", h1), MercadoLibreAdsEvidence("r5.csv", h2)))
        assertEquals(listOf("r6.csv", "r5.csv"), p.evidence.map { it.file })
        assertEquals("69c6d0497a19acc9db44ba89|PADS|2303516|2026-08-01|2026-08-05", p._id.id)
    }

    @Test
    fun pe2_unPeriodoNoCruzaUnMesNiSeInvierteNiVaSinEvidencia() {
        val ev = listOf(MercadoLibreAdsEvidence("r6.csv", h1))
        assertFailsWith<IllegalArgumentException> { periodo("2026-07-28", "2026-08-03", ev) }
        assertFailsWith<IllegalArgumentException> { periodo("2026-08-05", "2026-08-01", ev) }
        assertFailsWith<IllegalArgumentException> { periodo("2026-08-01", "2026-08-05", emptyList()) }
        assertFailsWith<IllegalArgumentException> { MercadoLibreAdsEvidence("r6.csv", "no-es-huella") }
        assertFailsWith<IllegalArgumentException> { MercadoLibreAdsEvidence(" ", h1) }
    }
}
