import com.fonrouge.commonLib.model.Money
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePrice
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePriceFetch
import com.fonrouge.mercadoLibreLib.model.SalePriceParse
import com.fonrouge.mercadoLibreLib.model.SalePriceVsListed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parser de `GET /items/{id}/sale_price`. Los cuerpos tienen la forma de la documentación oficial
 * («Precios de productos», 2026-10-14); los importes de EP-CHA10, EP-V25 y VB10 son los medidos el
 * 2026-11-03 en el reporte de publicaciones de ML. **No** son respuestas reales de la API: nadie la
 * ha llamado todavía con estos ítems.
 */
class MercadoLibreSalePriceTest {

    private fun parsed(body: String): MercadoLibreSalePrice {
        val r = MercadoLibreSalePrice.parse(body)
        assertIs<SalePriceParse.Parsed>(r, "se esperaba Parsed, llegó $r")
        return r.salePrice
    }

    private fun unreadable(body: String): String {
        val r = MercadoLibreSalePrice.parse(body)
        assertIs<SalePriceParse.Unreadable>(r, "se esperaba Unreadable, llegó $r")
        return r.reason
    }

    @Test
    fun `el ejemplo de la documentacion se lee completo`() {
        val p = parsed(
            """
            {"price_id":"145","amount":39200.0,"regular_amount":49000.0,"currency_id":"ARS",
             "reference_date":"2023-06-08T19:06:56Z",
             "metadata":{"promotion_id":"OFFER-MLA13456789-1122334455","promotion_type":"custom"}}
            """
        )
        assertEquals(Money(3_920_000), p.amount)
        assertEquals(Money(4_900_000), p.regularAmount)
        assertEquals("ARS", p.currencyId)
        assertEquals("145", p.priceId)
        assertEquals("2023-06-08T19:06:56Z", p.referenceDate)
        assertEquals("OFFER-MLA13456789-1122334455", p.promotionId)
        assertEquals("custom", p.promotionType)
        assertTrue(p.hasPromotion)
    }

    @Test
    fun `las tres publicaciones con promocion medidas dan su precio de venta al centavo`() {
        val chafer = parsed("""{"amount":636.22,"regular_amount":748.5,"currency_id":"MXN","metadata":{"promotion_type":"custom"}}""")
        assertEquals(Money(63_622), chafer.amount)
        assertEquals(Money(74_850), chafer.regularAmount)
        assertTrue(chafer.hasPromotion)

        val ev25 = parsed("""{"amount":423.2,"regular_amount":445.47,"currency_id":"MXN","metadata":{}}""")
        assertEquals(Money(42_320), ev25.amount)
        assertEquals(Money(44_547), ev25.regularAmount)
        assertTrue(ev25.hasPromotion)

        val vb10 = parsed("""{"amount":782.8,"regular_amount":824,"currency_id":"MXN"}""")
        assertEquals(Money(78_280), vb10.amount)
        assertEquals(Money(82_400), vb10.regularAmount)
        assertTrue(vb10.hasPromotion)
    }

    @Test
    fun `sin promocion no hay precio regular ni promocion`() {
        val p = parsed("""{"price_id":"1","amount":744.0,"regular_amount":null,"currency_id":"MXN","metadata":{}}""")
        assertEquals(Money(74_400), p.amount)
        assertNull(p.regularAmount)
        assertNull(p.promotionId)
        assertFalse(p.hasPromotion)

        val sinCampos = parsed("""{"amount":744,"currency_id":"MXN"}""")
        assertNull(sinCampos.regularAmount)
        assertFalse(sinCampos.hasPromotion)
    }

    @Test
    fun `un id de promocion sin precio regular mayor no rebaja nada`() {
        assertFalse(parsed("""{"amount":744,"currency_id":"MXN","metadata":{"promotion_id":"P-1"}}""").hasPromotion)
        assertFalse(parsed("""{"amount":744,"regular_amount":744,"currency_id":"MXN"}""").hasPromotion)
        assertFalse(parsed("""{"amount":744,"regular_amount":700,"currency_id":"MXN"}""").hasPromotion)
    }

    @Test
    fun `ceros a la derecha se aceptan y un importe como texto tambien`() {
        assertEquals(Money(63_622), parsed("""{"amount":636.2200,"currency_id":"MXN"}""").amount)
        assertEquals(Money(63_622), parsed("""{"amount":"636.22","currency_id":"MXN"}""").amount)
        assertEquals("145", parsed("""{"price_id":145,"amount":1,"currency_id":"MXN"}""").priceId)
    }

    @Test
    fun `un tercer decimal significativo se rechaza en vez de redondearse`() {
        assertTrue(unreadable("""{"amount":636.225,"currency_id":"MXN"}""").contains("amount"))
        assertTrue(unreadable("""{"amount":636.22,"regular_amount":748.505,"currency_id":"MXN"}""").contains("regular_amount"))
    }

    @Test
    fun `importes ausentes, nulos, no positivos o en notacion cientifica son ilegibles`() {
        unreadable("""{"currency_id":"MXN"}""")
        unreadable("""{"amount":null,"currency_id":"MXN"}""")
        unreadable("""{"amount":0,"currency_id":"MXN"}""")
        unreadable("""{"amount":-5,"currency_id":"MXN"}""")
        unreadable("""{"amount":6.3622E2,"currency_id":"MXN"}""")
        // Con ≤ 2 decimales en la mantisa, un exponente ignorado leería 7.44 donde ML dijo 744.00.
        unreadable("""{"amount":7.44E2,"currency_id":"MXN"}""")
        unreadable("""{"amount":1E3,"currency_id":"MXN"}""")
        unreadable("""{"amount":"","currency_id":"MXN"}""")
        unreadable("""{"amount":{"v":1},"currency_id":"MXN"}""")
    }

    @Test
    fun `un precio regular presente pero ilegible no se convierte en sin promocion`() {
        // Si se descartara en silencio, una venta en promoción aparecería como precio normal.
        unreadable("""{"amount":636.22,"regular_amount":"abc","currency_id":"MXN"}""")
        unreadable("""{"amount":636.22,"regular_amount":0,"currency_id":"MXN"}""")
    }

    @Test
    fun `sin moneda no se interpreta el importe`() {
        assertTrue(unreadable("""{"amount":636.22}""").contains("currency_id"))
        unreadable("""{"amount":636.22,"currency_id":"  "}""")
    }

    @Test
    fun `un cuerpo que no es un objeto JSON es ilegible`() {
        unreadable("no es json")
        unreadable("""[{"amount":1,"currency_id":"MXN"}]""")
        unreadable("")
    }

    // ---------- Contra el precio publicado (`MercadoLibreProduct.price`) ----------

    @Test
    fun `sin promocion el precio de venta es el publicado`() {
        val p = parsed("""{"amount":744.0,"regular_amount":null,"currency_id":"MXN"}""")
        assertEquals(SalePriceVsListed.NO_PROMOTION_SAME, p.compareWithListed(Money(74_400), "MXN"))
    }

    @Test
    fun `sin promocion y distinto del publicado es una anomalia`() {
        val p = parsed("""{"amount":744.0,"currency_id":"MXN"}""")
        assertEquals(SalePriceVsListed.NO_PROMOTION_DIFFERENT, p.compareWithListed(Money(75_000), "MXN"))
        // El día que ML retire `price` de `/items`, el sync lo deja en cero: también salta aquí.
        assertEquals(SalePriceVsListed.NO_PROMOTION_DIFFERENT, p.compareWithListed(Money.ZERO, "MXN"))
    }

    @Test
    fun `con promocion se compara el precio regular contra el publicado`() {
        val chafer = parsed("""{"amount":636.22,"regular_amount":748.5,"currency_id":"MXN"}""")
        assertEquals(SalePriceVsListed.PROMOTION_REGULAR_SAME, chafer.compareWithListed(Money(74_850), "MXN"))
        assertEquals(SalePriceVsListed.PROMOTION_REGULAR_DIFFERENT, chafer.compareWithListed(Money(80_000), "MXN"))
    }

    @Test
    fun `monedas distintas no se comparan`() {
        val p = parsed("""{"amount":744.0,"currency_id":"USD"}""")
        assertEquals(SalePriceVsListed.CURRENCY_DIFFERENT, p.compareWithListed(Money(74_400), "MXN"))
    }

    // ---------- Clasificación de la respuesta HTTP ----------

    @Test
    fun `la respuesta HTTP se clasifica sin refrescar nada`() {
        assertEquals(MercadoLibreSalePriceFetch.Found("{}"), MercadoLibreSalePriceFetch.classify(200, "{}"))
        assertIs<MercadoLibreSalePriceFetch.Retryable>(MercadoLibreSalePriceFetch.classify(200, "  "))
        assertIs<MercadoLibreSalePriceFetch.Retryable>(MercadoLibreSalePriceFetch.classify(200, null))
        assertEquals(MercadoLibreSalePriceFetch.NotFound, MercadoLibreSalePriceFetch.classify(404, """{"message":"x"}"""))
        assertEquals(MercadoLibreSalePriceFetch.Unauthorized, MercadoLibreSalePriceFetch.classify(401, null))
        assertEquals(MercadoLibreSalePriceFetch.Unauthorized, MercadoLibreSalePriceFetch.classify(403, null))
        assertIs<MercadoLibreSalePriceFetch.Retryable>(MercadoLibreSalePriceFetch.classify(429, null))
        assertIs<MercadoLibreSalePriceFetch.Retryable>(MercadoLibreSalePriceFetch.classify(500, null))
    }
}
