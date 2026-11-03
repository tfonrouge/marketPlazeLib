import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePrice
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceFetch
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceObservation
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceStatus
import com.fonrouge.mercadoLibreLib.model.SalePriceRefreshCycle
import com.fonrouge.mercadoLibreLib.model.planSalePriceRefresh
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MercadoLibreSalePriceRefreshTest {

    // Literales distintos: `OId()` sin argumento da el MISMO valor en Kotlin/JS.
    private val mp: MarketplaceId = OId("aaaaaaaaaaaaaaaaaaaaaaaa")
    private val t0 = Instant.parse("2026-11-03T18:00:00Z")
    private val t1 = Instant.parse("2026-11-03T19:00:00Z")

    private val promoChafer = """{"amount":636.22,"regular_amount":748.5,"currency_id":"MXN","metadata":{"promotion_type":"custom"}}"""
    private val sinPromo744 = """{"amount":744.0,"regular_amount":null,"currency_id":"MXN","metadata":{}}"""

    private fun product(id: String, price: Long = 74_400, status: String = "active") = MercadoLibreProduct(
        _id = StringId(id),
        marketplaceId = mp,
        title = id,
        price = Money(price),
        status = status,
    )

    private fun observation(
        id: String,
        status: MercadoLibreSalePriceStatus,
        lastAttemptAt: Instant,
        body: String? = sinPromo744,
    ): MercadoLibreSalePriceObservation {
        val price = body?.let { (MercadoLibreSalePrice.parse(it) as com.fonrouge.mercadoLibreLib.model.SalePriceParse.Parsed).salePrice }
        return MercadoLibreSalePriceObservation(
            _id = StringId(id),
            marketplaceId = mp,
            status = status,
            salePrice = price,
            observedAt = price?.let { lastAttemptAt },
            lastAttemptAt = lastAttemptAt,
        )
    }

    // ---------- Transición de una observación ----------

    @Test
    fun `un precio obtenido queda vigente con su fecha`() {
        val o = assertNotNull(
            MercadoLibreSalePriceObservation.next(null, "MLM5042202116", mp, MercadoLibreSalePriceFetch.Found(promoChafer), t0)
        )
        assertEquals("MLM5042202116", o._id.id)
        assertEquals(mp, o.marketplaceId)
        assertEquals(MercadoLibreSalePrice.CONTEXT_MARKETPLACE, o.context)
        assertEquals(MercadoLibreSalePriceStatus.OBSERVED, o.status)
        assertEquals(Money(63_622), o.salePrice?.amount)
        assertEquals(t0, o.observedAt)
        assertEquals(t0, o.lastAttemptAt)
        assertNull(o.lastError)
        assertTrue(o.isCurrent)
    }

    @Test
    fun `un fallo posterior conserva el ultimo precio pero deja de ser vigente`() {
        val antes = observation("MLM1", MercadoLibreSalePriceStatus.OBSERVED, t0)
        val casos = listOf(
            MercadoLibreSalePriceFetch.NotFound to MercadoLibreSalePriceStatus.NOT_AVAILABLE,
            MercadoLibreSalePriceFetch.Retryable("HTTP 503") to MercadoLibreSalePriceStatus.RETRYABLE_ERROR,
            MercadoLibreSalePriceFetch.Found("""{"amount":1.234,"currency_id":"MXN"}""") to MercadoLibreSalePriceStatus.UNREADABLE,
        )
        for ((outcome, esperado) in casos) {
            val o = assertNotNull(MercadoLibreSalePriceObservation.next(antes, "MLM1", mp, outcome, t1))
            assertEquals(esperado, o.status, "con $outcome")
            assertEquals(antes.salePrice, o.salePrice, "con $outcome el último precio se conserva")
            assertEquals(t0, o.observedAt, "con $outcome la fecha del precio no cambia")
            assertEquals(t1, o.lastAttemptAt)
            assertFalse(o.isCurrent, "con $outcome el precio ya no es de hoy")
        }
    }

    @Test
    fun `los motivos de fallo quedan escritos`() {
        val retry = MercadoLibreSalePriceObservation.next(null, "MLM1", mp, MercadoLibreSalePriceFetch.Retryable("HTTP 503"), t0)
        assertEquals("HTTP 503", retry?.lastError)
        val ilegible = MercadoLibreSalePriceObservation.next(null, "MLM1", mp, MercadoLibreSalePriceFetch.Found("{}"), t0)
        assertEquals(MercadoLibreSalePriceStatus.UNREADABLE, ilegible?.status)
        assertTrue(ilegible?.lastError.orEmpty().contains("amount"))
        val sinPrecio = MercadoLibreSalePriceObservation.next(null, "MLM1", mp, MercadoLibreSalePriceFetch.NotFound, t0)
        assertEquals(MercadoLibreSalePriceStatus.NOT_AVAILABLE, sinPrecio?.status)
        assertNull(sinPrecio?.salePrice)
    }

    @Test
    fun `al recuperarse se borra el error`() {
        val antes = observation("MLM1", MercadoLibreSalePriceStatus.RETRYABLE_ERROR, t0).copy(lastError = "HTTP 503")
        val o = assertNotNull(MercadoLibreSalePriceObservation.next(antes, "MLM1", mp, MercadoLibreSalePriceFetch.Found(promoChafer), t1))
        assertEquals(MercadoLibreSalePriceStatus.OBSERVED, o.status)
        assertNull(o.lastError)
        assertEquals(t1, o.observedAt)
    }

    @Test
    fun `un token rechazado no produce nada que escribir`() {
        val antes = observation("MLM1", MercadoLibreSalePriceStatus.OBSERVED, t0)
        assertNull(MercadoLibreSalePriceObservation.next(antes, "MLM1", mp, MercadoLibreSalePriceFetch.Unauthorized, t1))
        assertNull(MercadoLibreSalePriceObservation.next(null, "MLM1", mp, MercadoLibreSalePriceFetch.Unauthorized, t1))
    }

    // ---------- Plan ----------

    @Test
    fun `solo se consultan publicaciones activas o pausadas`() {
        val plan = planSalePriceRefresh(
            listOf(
                product("MLM1", status = "active"),
                product("MLM2", status = "paused"),
                product("MLM3", status = "closed"),
                product("MLM4", status = "under_review"),
                product("MLM5", status = "inactive"),
                product("MLM6", status = "algo_nuevo"),
            ),
            previous = emptyMap(),
            quota = 100,
        )
        assertEquals(listOf("MLM1", "MLM2"), plan)
    }

    @Test
    fun `primero las nunca consultadas, luego las no vigentes, luego la mas vieja`() {
        val previous = mapOf(
            "MLM1" to observation("MLM1", MercadoLibreSalePriceStatus.OBSERVED, t1),
            "MLM2" to observation("MLM2", MercadoLibreSalePriceStatus.OBSERVED, t0),
            "MLM3" to observation("MLM3", MercadoLibreSalePriceStatus.RETRYABLE_ERROR, t1),
            "MLM4" to observation("MLM4", MercadoLibreSalePriceStatus.NOT_AVAILABLE, t0, body = null),
        )
        val plan = planSalePriceRefresh(
            listOf("MLM1", "MLM2", "MLM3", "MLM4", "MLM5").map { product(it) },
            previous,
            quota = 100,
        )
        assertEquals(listOf("MLM5", "MLM4", "MLM3", "MLM2", "MLM1"), plan)
    }

    @Test
    fun `el tope se aplica despues de ordenar y no hay repetidas`() {
        val previous = mapOf("MLM1" to observation("MLM1", MercadoLibreSalePriceStatus.OBSERVED, t0))
        val products = listOf(product("MLM1"), product("MLM2"), product("MLM2"), product("MLM3"))
        assertEquals(listOf("MLM2", "MLM3"), planSalePriceRefresh(products, previous, quota = 2))
        assertEquals(emptyList(), planSalePriceRefresh(products, previous, quota = 0))
    }

    // ---------- Ciclo ----------

    @Test
    fun `un 401 o 403 en la primera publicacion corta el ciclo sin escribir`() {
        val products = listOf(product("MLM1"), product("MLM2"))
        val cycle = SalePriceRefreshCycle(planned = 2, listed = products.associateBy { it._id.id })
        val step = cycle.record(null, "MLM1", mp, MercadoLibreSalePriceFetch.Unauthorized, t0)
        assertNull(step.toSave)
        assertTrue(step.stop)
        assertTrue(cycle.report.stoppedBy.orEmpty().contains("401/403"))
        assertEquals(0, cycle.report.observed + cycle.report.retryable + cycle.report.notAvailable + cycle.report.unreadable)
    }

    @Test
    fun `un 401 a mitad del ciclo conserva lo ya observado y corta`() {
        val products = listOf(product("MLM1"), product("MLM2"), product("MLM3"))
        val cycle = SalePriceRefreshCycle(planned = 3, listed = products.associateBy { it._id.id })
        assertFalse(cycle.record(null, "MLM1", mp, MercadoLibreSalePriceFetch.Found(sinPromo744), t0).stop)
        val corte = cycle.record(null, "MLM2", mp, MercadoLibreSalePriceFetch.Unauthorized, t0)
        assertTrue(corte.stop)
        assertNull(corte.toSave)
        assertEquals(1, cycle.report.observed)
    }

    @Test
    fun `cinco fallos transitorios seguidos cortan el ciclo y se registran`() {
        val products = (1..7).map { product("MLM$it") }
        val cycle = SalePriceRefreshCycle(planned = 7, listed = products.associateBy { it._id.id })
        val pasos = (1..5).map { cycle.record(null, "MLM$it", mp, MercadoLibreSalePriceFetch.Retryable("HTTP 503"), t0) }
        assertEquals(listOf(false, false, false, false, true), pasos.map { it.stop })
        assertTrue(pasos.all { it.toSave?.status == MercadoLibreSalePriceStatus.RETRYABLE_ERROR })
        assertEquals(5, cycle.report.retryable)
        assertTrue(cycle.report.stoppedBy.orEmpty().contains("5"))
    }

    @Test
    fun `un exito entre fallos reinicia la cuenta de seguidos`() {
        val products = (1..9).map { product("MLM$it") }
        val cycle = SalePriceRefreshCycle(planned = 9, listed = products.associateBy { it._id.id })
        repeat(4) { cycle.record(null, "MLM$it", mp, MercadoLibreSalePriceFetch.Retryable("HTTP 503"), t0) }
        cycle.record(null, "MLM5", mp, MercadoLibreSalePriceFetch.Found(sinPromo744), t0)
        val despues = (6..9).map { cycle.record(null, "MLM$it", mp, MercadoLibreSalePriceFetch.Retryable("HTTP 503"), t0) }
        assertTrue(despues.none { it.stop })
        assertNull(cycle.report.stoppedBy)
    }

    @Test
    fun `las tres publicaciones con promocion y una sin promocion, como en el corte del 14 de octubre`() {
        // Publicado (`/items.price`) y precio de venta medidos el 2026-11-03.
        val chafer = product("MLM5042202116", price = 74_850)
        val ev25 = product("MLM2817997317", price = 44_547)
        val vb10 = product("MLM2798037473", price = 82_400)
        val normal = product("MLM9000000001", price = 74_400)
        val listed = listOf(chafer, ev25, vb10, normal).associateBy { it._id.id }
        val cycle = SalePriceRefreshCycle(planned = 4, listed = listed)
        val bodies = mapOf(
            chafer._id.id to promoChafer,
            ev25._id.id to """{"amount":423.2,"regular_amount":445.47,"currency_id":"MXN"}""",
            vb10._id.id to """{"amount":782.8,"regular_amount":824,"currency_id":"MXN"}""",
            normal._id.id to sinPromo744,
        )
        val guardadas = bodies.map { (id, body) ->
            id to assertNotNull(cycle.record(null, id, mp, MercadoLibreSalePriceFetch.Found(body), t0).toSave)
        }.toMap()

        assertEquals(Money(63_622), guardadas.getValue(chafer._id.id).salePrice?.amount)
        assertEquals(Money(42_320), guardadas.getValue(ev25._id.id).salePrice?.amount)
        assertEquals(Money(78_280), guardadas.getValue(vb10._id.id).salePrice?.amount)
        // Sin promoción, el precio de venta es el publicado.
        assertEquals(normal.price, guardadas.getValue(normal._id.id).salePrice?.amount)

        assertEquals(4, cycle.report.observed)
        assertEquals(3, cycle.report.withPromotion)
        assertEquals(0, cycle.report.noPromotionDifferent.size)
        assertNull(cycle.report.stoppedBy)
    }

    @Test
    fun `una publicacion sin promocion con precio distinto del publicado se nombra en el reporte`() {
        val p = product("MLM1", price = 75_000)
        val cycle = SalePriceRefreshCycle(planned = 1, listed = mapOf("MLM1" to p))
        cycle.record(null, "MLM1", mp, MercadoLibreSalePriceFetch.Found(sinPromo744), t0)
        assertEquals(listOf("MLM1"), cycle.report.noPromotionDifferent)
        assertTrue(cycle.report.toString().contains("MLM1"))
    }

    // ---------- Serialización ----------

    @Test
    fun `los instantes de la observacion se serializan como texto ISO`() {
        // Un solo tipo para los instantes: el documento completo los escribe como texto (precedente D100 de
        // mppErsaPack, donde un `$set` suelto los escribió como fecha BSON y los lectores reventaron).
        val o = assertNotNull(MercadoLibreSalePriceObservation.next(null, "MLM1", mp, MercadoLibreSalePriceFetch.Found(promoChafer), t0))
        val json = Json.encodeToJsonElement(MercadoLibreSalePriceObservation.serializer(), o).jsonObject
        assertEquals(t0.toString(), json["observedAt"]?.jsonPrimitive?.content)
        assertTrue(json["observedAt"]?.jsonPrimitive?.isString == true)
        assertEquals(t0.toString(), json["lastAttemptAt"]?.jsonPrimitive?.content)
        assertEquals("observed", json["status"]?.jsonPrimitive?.content)
        assertEquals(o, Json.decodeFromJsonElement(MercadoLibreSalePriceObservation.serializer(), json))
    }

    @Test
    fun `el precio de venta hidratado viaja en el JSON del producto`() {
        // Es lo que la RPC le manda al navegador: si el campo del cuerpo no se serializara, la columna
        // saldría vacía aunque el lookup lo trajera.
        val p = product("MLM5042202116", price = 74_850)
        p.salePrice = MercadoLibreSalePriceObservation.next(null, p._id.id, mp, MercadoLibreSalePriceFetch.Found(promoChafer), t0)
        val json = Json.encodeToString(MercadoLibreProduct.serializer(), p)
        val back = Json.decodeFromString(MercadoLibreProduct.serializer(), json)
        assertEquals(Money(63_622), back.salePrice?.salePrice?.amount)
        assertTrue(back.salePrice?.isCurrent == true)
        assertNull(Json.decodeFromString(MercadoLibreProduct.serializer(), Json.encodeToString(MercadoLibreProduct.serializer(), product("MLM2"))).salePrice)
    }

    @Test
    fun `el reporte cuenta cada desenlace`() {
        val products = (1..4).map { product("MLM$it") }
        val cycle = SalePriceRefreshCycle(planned = 4, listed = products.associateBy { it._id.id })
        cycle.record(null, "MLM1", mp, MercadoLibreSalePriceFetch.Found(sinPromo744), t0)
        cycle.record(null, "MLM2", mp, MercadoLibreSalePriceFetch.NotFound, t0)
        cycle.record(null, "MLM3", mp, MercadoLibreSalePriceFetch.Found("{}"), t0)
        cycle.record(null, "MLM4", mp, MercadoLibreSalePriceFetch.Retryable("HTTP 500"), t0)
        val r = cycle.report
        assertEquals(4, r.planned)
        assertEquals(1, r.observed)
        assertEquals(1, r.notAvailable)
        assertEquals(1, r.unreadable)
        assertEquals(1, r.retryable)
        assertEquals(0, r.withPromotion)
    }
}
