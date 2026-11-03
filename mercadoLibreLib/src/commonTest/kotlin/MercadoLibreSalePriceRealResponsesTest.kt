import com.fonrouge.commonLib.model.Money
import com.fonrouge.mercadoLibreLib.model.MercadoLibreSalePrice
import com.fonrouge.mercadoLibreLib.model.SalePriceParse
import com.fonrouge.mercadoLibreLib.model.SalePriceVsListed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Respuestas **reales** de `GET /items/{id}/sale_price?context=channel_marketplace`, obtenidas el
 * 2026-11-03 19:19Z con el token de producción de mppErsaPack (lectura, sin refrescarlo), junto con el
 * `price` publicado de cada ítem en ese momento. Cuerpos tal cual, salvo los números de la oferta y de la
 * campaña de VB10, enmascarados con ceros.
 *
 * Lo que mostró la realidad frente a la documentación: sin promoción `regular_amount` llega `null` y
 * `metadata` vacío; con promoción de campaña `metadata` trae además `campaign_id`, que la documentación no
 * menciona (se ignora); un `amount` entero llega sin decimales (`453`); una publicación pausada también
 * responde 200 con su precio.
 */
class MercadoLibreSalePriceRealResponsesTest {

    private data class Real(val sku: String, val body: String, val listedCents: Long)

    private val vb10 = Real(
        "VB10",
        """{"price_id":"11","amount":782.8,"regular_amount":824.0,"currency_id":"MXN","reference_date":"2026-11-03T19:19:46Z","metadata":{"campaign_id":"P-MLM00000000","promotion_id":"OFFER-MLM2798037473-00000000000","promotion_type":"campaign"}}""",
        82_400,
    )
    private val sinPromocion = listOf(
        Real("EP-CHA10", """{"price_id":"4","amount":748.5,"regular_amount":null,"currency_id":"MXN","reference_date":"2026-11-03T19:19:45Z","metadata":{}}""", 74_850),
        Real("EP-V25", """{"price_id":"5","amount":445.47,"regular_amount":null,"currency_id":"MXN","reference_date":"2026-11-03T19:19:45Z","metadata":{}}""", 44_547),
        Real("JC05", """{"price_id":"7","amount":453,"regular_amount":null,"currency_id":"MXN","reference_date":"2026-11-03T19:19:46Z","metadata":{}}""", 45_300),
        // Pausada: también 200.
        Real("CUB-01", """{"price_id":"6","amount":331.2,"regular_amount":null,"currency_id":"MXN","reference_date":"2026-11-03T19:19:47Z","metadata":{}}""", 33_120),
    )

    private fun parsed(r: Real): MercadoLibreSalePrice {
        val p = MercadoLibreSalePrice.parse(r.body)
        assertIs<SalePriceParse.Parsed>(p, "${r.sku}: $p")
        return p.salePrice
    }

    @Test
    fun `VB10 en campana da su precio de venta y el regular es el publicado`() {
        val p = parsed(vb10)
        assertEquals(Money(78_280), p.amount)
        assertEquals(Money(82_400), p.regularAmount)
        assertEquals("campaign", p.promotionType)
        assertEquals("OFFER-MLM2798037473-00000000000", p.promotionId)
        assertEquals("11", p.priceId)
        assertTrue(p.hasPromotion)
        assertEquals(SalePriceVsListed.PROMOTION_REGULAR_SAME, p.compareWithListed(Money(vb10.listedCents), "MXN"))
    }

    @Test
    fun `sin promocion el precio de venta real es el publicado en las cuatro`() {
        for (r in sinPromocion) {
            val p = parsed(r)
            assertFalse(p.hasPromotion, r.sku)
            assertNull(p.regularAmount, r.sku)
            assertNull(p.promotionId, r.sku)
            assertEquals(Money(r.listedCents), p.amount, r.sku)
            assertEquals(SalePriceVsListed.NO_PROMOTION_SAME, p.compareWithListed(Money(r.listedCents), "MXN"), r.sku)
        }
    }
}
