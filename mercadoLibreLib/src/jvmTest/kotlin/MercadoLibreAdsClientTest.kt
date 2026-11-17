import com.fonrouge.base.types.OId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.Money
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsClient
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsDayStore
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreAdsRefresher
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDay
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsDayStatus
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsIdentity
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsReadAction
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsRequest
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsResponse
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSavedState
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsSource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `MercadoLibreAdsClient` y `MercadoLibreAdsRefresher` (PublicidadMercadoAds B4-06 y B4-07) contra un motor HTTP falso
 * que sirve las respuestas **reales** del 6-sep (`MercadoLibreAdsFixturesSep06`) y cuenta cada solicitud que de verdad
 * sale por Ktor. Para otras fechas sirve las mismas respuestas con la fecha pedida en el total diario, el único lugar
 * donde aparece la fecha.
 */
class MercadoLibreAdsClientTest {

    private val token = "APP_USR-secreto-de-prueba-123"
    private val marketplace = Marketplace(name = "ERSAPACK", type = "MercadoLibre", connectionConfig = mapOf("accessToken" to token))
    private val identity = MercadoLibreAdsIdentity(OId("69c6d0497a19acc9db44ba89"), MercadoLibreAdsProduct.PADS, 2303516, "MLM", "MXN")
    private val f = MercadoLibreAdsFixturesSep06

    /** Motor falso: registra cada solicitud y responde lo que diga [estado] para ella, con el cuerpo real. */
    private inner class Api(val estado: (HttpRequestData) -> Int = { 200 }) {
        val pedidas = mutableListOf<HttpRequestData>()
        val engine = MockEngine { req -> pedidas += req; responder(req) }

        private fun MockRequestHandleScope.responder(req: HttpRequestData): HttpResponseData {
            val s = estado(req)
            val cuerpo = if (s == 200) cuerpoReal(req) else """{"message":"error $s","status":$s}"""
            return respond(cuerpo, HttpStatusCode.fromValue(s), headersOf(HttpHeaders.ContentType, "application/json"))
        }

        private fun cuerpoReal(req: HttpRequestData): String {
            val path = req.url.encodedPath
            val p = req.url.parameters
            return when {
                path.endsWith("/ad_groups/search") -> when (p["offset"]) { null -> f.adGroupsOffset0; "50" -> f.adGroupsOffset50; else -> fail("offset ${p["offset"]}") }
                path.endsWith("/campaigns/search") -> f.campaignsDaily.replace("2026-09-06", p["date_from"]!!)
                path.endsWith("/2260824327/ads") -> f.ads2260824327
                path.endsWith("/2660965675/ads") -> f.ads2660965675
                path.endsWith("/2260754914/ads") -> f.ads2260754914
                path.endsWith("/3023399752/ads") -> f.ads3023399752
                else -> fail("ruta inesperada $path")
            }
        }
    }

    /** Almacén en memoria que registra si alguien lo tocó. */
    private class Memoria(val dias: MutableMap<String, MercadoLibreAdsDay> = mutableMapOf()) : MercadoLibreAdsDayStore {
        var accesos = 0
        override suspend fun savedStates(identity: MercadoLibreAdsIdentity): Map<String, MercadoLibreAdsSavedState> {
            accesos++
            return dias.mapValues { (_, d) -> MercadoLibreAdsSavedState(d.status, d.source, d.lastReadAt.take(10)) }
        }
        override suspend fun load(identity: MercadoLibreAdsIdentity, date: String): MercadoLibreAdsDay? { accesos++; return dias[date] }
        override suspend fun replaceIf(day: MercadoLibreAdsDay, expected: MercadoLibreAdsDay?): Boolean {
            accesos++
            val actual = dias[day.date]
            val vigente = if (expected == null) actual == null else actual != null && actual.revision == expected.revision && actual.lastReadAt == expected.lastReadAt
            if (vigente) dias[day.date] = day
            return vigente
        }
    }

    private val hoy = LocalDate(2026, 11, 12)

    private fun refrescar(api: Api, memoria: Memoria, cupo: Int, enabledFrom: String? = "2026-11-12", ahora: String = "2026-11-12T15:00:00Z") =
        runBlocking {
            MercadoLibreAdsRefresher(MercadoLibreAdsClient(api.engine), memoria).refresh(
                marketplace, identity, enabledFrom, hoy, 9, ahora, rereadDays = 14, quotaRequests = cupo, pauseMillis = 0,
            )
        }

    // ------------------------------------------------------------------ B4-06: el cliente

    @Test
    fun c1_unDiaRealSonSeisGetConLasRutasParametrosYCabecerasDeLaSonda() = runBlocking<Unit> {
        val api = Api()
        val cliente = MercadoLibreAdsClient(api.engine)
        val pedidas = listOf(
            MercadoLibreAdsRequest.AdGroupsPage("2026-09-06", 0),
            MercadoLibreAdsRequest.AdGroupsPage("2026-09-06", 50),
            MercadoLibreAdsRequest.CampaignsDaily("2026-09-06"),
            MercadoLibreAdsRequest.AdGroupAds("2026-09-06", 2660965675L, 0),
        )
        for (r in pedidas) assertIs<MercadoLibreAdsResponse.Ok>(cliente.get(marketplace, 2303516, r))
        val m = "clicks,prints,cost,cpc,ctr,direct_amount,indirect_amount,total_amount,direct_units_quantity,indirect_units_quantity,units_quantity"
        assertEquals(
            listOf(
                "https://api.mercadolibre.com/advertising/MLM/advertisers/2303516/product_ads/ad_groups/search?date_from=2026-09-06&date_to=2026-09-06&metrics=${m.uppercase()}&metrics_summary=true&limit=50",
                "https://api.mercadolibre.com/advertising/MLM/advertisers/2303516/product_ads/ad_groups/search?date_from=2026-09-06&date_to=2026-09-06&metrics=${m.uppercase()}&metrics_summary=true&limit=50&offset=50",
                "https://api.mercadolibre.com/advertising/MLM/advertisers/2303516/product_ads/campaigns/search?date_from=2026-09-06&date_to=2026-09-06&metrics=$m&aggregation_type=DAILY&limit=50",
                "https://api.mercadolibre.com/advertising/MLM/product_ads/ad_groups/2660965675/ads?date_from=2026-09-06&date_to=2026-09-06&metrics=$m",
            ),
            api.pedidas.map { it.url.toString() },
        )
        assertTrue(api.pedidas.all { it.method == HttpMethod.Get })
        assertTrue(api.pedidas.all { it.headers[HttpHeaders.Authorization] == "Bearer $token" && it.headers["Api-Version"] == "2" })
        assertTrue(api.pedidas.none { token in it.url.toString() })
    }

    @Test
    fun c2_estadosHttpSegunS1() = runBlocking<Unit> {
        val r = MercadoLibreAdsRequest.CampaignsDaily("2026-09-06")
        fun con(s: Int) = runBlocking { MercadoLibreAdsClient(Api { s }.engine).get(marketplace, 2303516, r) }
        for (s in listOf(400, 401, 403, 404, 429)) assertEquals(s, (con(s) as MercadoLibreAdsResponse.Cut).status, "estado $s")
        for (s in listOf(500, 502, 503)) assertIs<MercadoLibreAdsResponse.Retryable>(con(s), "estado $s")
        val ilegible = MercadoLibreAdsClient(MockEngine { respond("<html>gateway</html>", HttpStatusCode.OK) }).get(marketplace, 2303516, r)
        assertIs<MercadoLibreAdsResponse.Retryable>(ilegible)
        val caido = MercadoLibreAdsClient(MockEngine { throw java.io.IOException("conexión reiniciada") }).get(marketplace, 2303516, r)
        assertIs<MercadoLibreAdsResponse.Retryable>(caido)
    }

    @Test
    fun c3_advertisersUsaApiVersion1Y404EsProductoNoHabilitado() = runBlocking<Unit> {
        val api = Api { req -> if (req.url.parameters["product_id"] == "DISPLAY") 404 else 200 }
        val cliente = MercadoLibreAdsClient(MockEngine { req ->
            api.pedidas += req
            if (req.url.parameters["product_id"] == "DISPLAY") respond("{}", HttpStatusCode.NotFound)
            else respond("""{"advertisers":[{"advertiser_id":2303516,"site_id":"MLM"}]}""", HttpStatusCode.OK)
        })
        assertSame(MercadoLibreAdsResponse.NotEnabled, cliente.advertisers(marketplace, MercadoLibreAdsProduct.DISPLAY))
        assertIs<MercadoLibreAdsResponse.Ok>(cliente.advertisers(marketplace, MercadoLibreAdsProduct.PADS))
        assertIs<MercadoLibreAdsResponse.Ok>(cliente.bonifications(marketplace))
        assertEquals(
            listOf(
                "https://api.mercadolibre.com/advertising/advertisers?product_id=DISPLAY",
                "https://api.mercadolibre.com/advertising/advertisers?product_id=PADS",
                "https://api.mercadolibre.com/advertising/advertisers/bonifications",
            ),
            api.pedidas.map { it.url.toString() },
        )
        assertTrue(api.pedidas.all { it.headers["Api-Version"] == "1" && it.method == HttpMethod.Get })
    }

    @Test
    fun c4_elTokenNuncaSaleNiEnErroresNiEnExcepciones() = runBlocking<Unit> {
        val r = MercadoLibreAdsRequest.CampaignsDaily("2026-09-06")
        val eco = MercadoLibreAdsClient(MockEngine { req ->
            respond("""{"message":"token ${req.headers[HttpHeaders.Authorization]} inválido"}""", HttpStatusCode.BadRequest)
        }).get(marketplace, 2303516, r) as MercadoLibreAdsResponse.Cut
        assertFalse(token in eco.reason, eco.reason)
        assertTrue("***" in eco.reason, eco.reason)
        val excepcion = MercadoLibreAdsClient(MockEngine { throw IllegalStateException("falló con $token") })
            .get(marketplace, 2303516, r) as MercadoLibreAdsResponse.Retryable
        assertFalse(token in excepcion.reason, excepcion.reason)
    }

    @Test
    fun c5_sinTokenCortaSinLlamar() = runBlocking<Unit> {
        val api = Api()
        val sinToken = Marketplace(name = "ERSAPACK", type = "MercadoLibre")
        val r = MercadoLibreAdsClient(api.engine).get(sinToken, 2303516, MercadoLibreAdsRequest.CampaignsDaily("2026-09-06"))
        assertIs<MercadoLibreAdsResponse.Cut>(r)
        assertEquals(0, api.pedidas.size)
    }

    @Test
    fun c6_listaBlanca() {
        val ok = listOf(
            "/advertising/advertisers", "/advertising/advertisers/bonifications",
            "/advertising/MLM/advertisers/2303516/product_ads/ad_groups/search", "/advertising/MLM/product_ads/ad_groups/1/ads",
        )
        val no = listOf(
            "/oauth/token", "/items/MLM1", "/advertising/MLM/advertisers/2303516/../../oauth/token", "/advertising/MLA/product_ads/x",
            "/advertising/advertisers/2303516/display/campaigns", "/users/me", "/advertising/MLM/product_ads/oauth/token",
            "/advertising/MLM/product_ads/../../../items/MLM1",
        )
        assertTrue(ok.all { MercadoLibreAdsClient.isAllowedPath(it) })
        assertTrue(no.none { MercadoLibreAdsClient.isAllowedPath(it) }, no.filter { MercadoLibreAdsClient.isAllowedPath(it) }.toString())
    }

    // ------------------------------------------------------------------ B4-07: el ciclo con el cliente y un almacén

    @Test
    fun r1_conElInterruptorApagadoCeroLlamadasYNiSeLeeElAlmacen() {
        val api = Api()
        val memoria = Memoria()
        val r = refrescar(api, memoria, cupo = 60, enabledFrom = null)
        assertEquals("interruptor apagado", r.skippedBy)
        assertEquals(0, api.pedidas.size)
        assertEquals(0, memoria.accesos)
    }

    @Test
    fun r2_elCupoSeMideEnSolicitudesHttpConPaginasYDesglose() {
        val api = Api()
        val memoria = Memoria()
        val r = refrescar(api, memoria, cupo = 14)
        assertEquals(14, api.pedidas.size)
        assertEquals(14, r.requests)
        val porTipo = api.pedidas.groupingBy {
            when {
                it.url.encodedPath.endsWith("/ad_groups/search") -> "páginas"
                it.url.encodedPath.endsWith("/campaigns/search") -> "total diario"
                else -> "desglose"
            }
        }.eachCount()
        assertEquals(mapOf("páginas" to 6, "total diario" to 2, "desglose" to 6), porTipo)
        assertEquals(setOf("2026-08-14", "2026-08-15"), memoria.dias.keys) // los más viejos de la ventana
        assertEquals("2026-08-16", r.quotaExhaustedAt)
        assertTrue(memoria.dias.values.all { it.total == Money(20_657) && it.source == MercadoLibreAdsSource.API })
    }

    @Test
    fun r3_un429CortaSinMasLlamadasYElSiguienteCicloRetoma() {
        val memoria = Memoria()
        val api429 = Api { req -> if (req.url.parameters["date_from"] == "2026-08-15" && req.url.parameters["offset"] == "50") 429 else 200 }
        val r1 = refrescar(api429, memoria, cupo = 60, ahora = "2026-11-12T15:00:00Z")
        assertEquals("2026-08-15" to 429, r1.cut)
        assertEquals(8, api429.pedidas.size)
        assertEquals(setOf("2026-08-14"), memoria.dias.keys)
        val api = Api()
        val r2 = refrescar(api, memoria, cupo = 60, ahora = "2026-11-12T16:00:00Z")
        assertEquals(null, r2.cut)
        assertEquals(listOf("2026-08-15", "2026-08-16"), memoria.dias.keys.sorted().drop(1).take(2))
        assertEquals("2026-08-15", api.pedidas.first().url.parameters["date_from"])
        assertEquals(10, memoria.dias.size - 1) // 60 solicitudes = 10 días nuevos
    }

    @Test
    fun r4_unDiaParcialConservaLaUltimaRevisionValida() {
        // El 05-nov ya está guardado de la API y toca releerlo; un 503 a la mitad de la relectura salta el día sin tocarlo.
        val memoria = Memoria()
        refrescar(Api(), memoria, cupo = 60) // guarda del 14 al 23-ago
        val previo = memoria.dias.getValue("2026-08-14").copy(lastReadAt = "2026-11-11T15:00:00Z")
        memoria.dias.clear()
        memoria.dias["2026-11-05"] = previo.copy(date = "2026-11-05", _id = com.fonrouge.base.types.StringId(MercadoLibreAdsDay.idOf(identity, "2026-11-05")))
        val antes = memoria.dias.getValue("2026-11-05")
        // todos los días de la ventana faltan salvo el 05-nov, que es relectura: va al final del plan.
        val api = Api { req -> if (req.url.parameters["date_from"] == "2026-11-05" && req.url.encodedPath.endsWith("/ads")) 503 else 200 }
        val r = runBlocking {
            MercadoLibreAdsRefresher(MercadoLibreAdsClient(api.engine), memoria).refresh(
                marketplace, identity, "x", hoy, 9, "2026-11-12T15:00:00Z", rereadDays = 14, quotaRequests = 6 * 90, pauseMillis = 0,
            )
        }
        assertEquals(listOf("2026-11-05"), r.retryable)
        assertSame(antes, memoria.dias["2026-11-05"])
        assertEquals(1, antes.revision)
        assertEquals(MercadoLibreAdsReadAction.CREATED, r.actions["2026-08-14"])
    }

    @Test
    fun r5_unDiaVencidoSeCierraSinLlamar() {
        val memoria = Memoria()
        refrescar(Api(), memoria, cupo = 6) // guarda el 14-ago
        val dia = memoria.dias.getValue("2026-08-14")
        assertEquals(MercadoLibreAdsDayStatus.PROVISIONAL, dia.status)
        val api = Api()
        val r = refrescar(api, memoria, cupo = 0) // sin cupo: sólo cierra
        assertEquals(listOf("2026-08-14"), r.closed)
        assertEquals(MercadoLibreAdsDayStatus.CERRADO, memoria.dias.getValue("2026-08-14").status)
        assertEquals(dia.revision, memoria.dias.getValue("2026-08-14").revision)
        assertEquals(0, api.pedidas.size)
    }
}
