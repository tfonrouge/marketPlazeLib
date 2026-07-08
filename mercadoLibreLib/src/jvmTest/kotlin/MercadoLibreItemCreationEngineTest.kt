import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.engine.MercadoLibreItemCreationEngine
import com.fonrouge.mercadoLibreLib.model.MercadoLibreItemPayload
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MercadoLibreItemCreationEngineTest {

    private val marketplace = Marketplace(
        name = "Test MercadoLibre",
        type = "MercadoLibre",
        connectionConfig = mapOf("accessToken" to "APP_USR-token"),
    )

    private val payload = MercadoLibreItemPayload(
        title = "Cuchara Pastelera Biodegradable 25 pzs",
        categoryId = "MLM167574",
        price = 49.90,
        currencyId = "MXN",
        availableQuantity = 10,
        condition = "new",
        listingTypeId = "gold_special",
        attributes = mapOf("BRAND" to "ErsaPack"),
        pictureUrls = listOf("https://example.com/photo.jpg"),
    )

    @Test
    fun `createItem sends POST with bearer token and full body`() = runBlocking {
        var capturedUrl = ""
        var capturedAuth = ""
        var capturedBody = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization] ?: ""
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"id":"MLM999888777","permalink":"https://articulo.mercadolibre.com.mx/MLM999888777"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(result.isOk, "expected success, got: ${result.msgError}")
        assertEquals("MLM999888777", result.itemId)
        assertEquals("https://articulo.mercadolibre.com.mx/MLM999888777", result.permalink)
        assertEquals("https://api.mercadolibre.com/items", capturedUrl)
        assertEquals("Bearer APP_USR-token", capturedAuth)
        // La API de ML crea por familia: se manda family_name (del título) y NO title (D70).
        assertTrue(capturedBody.contains(""""family_name":"Cuchara Pastelera Biodegradable 25 pzs""""))
        assertTrue(!capturedBody.contains(""""title":"""), "no debe mandar title (ML lo rechaza): $capturedBody")
        assertTrue(capturedBody.contains(""""category_id":"MLM167574""""))
        assertTrue(capturedBody.contains(""""price":49.9"""))
        assertTrue(capturedBody.contains(""""listing_type_id":"gold_special""""))
        assertTrue(capturedBody.contains(""""id":"BRAND","value_name":"ErsaPack""""))
        assertTrue(capturedBody.contains(""""source":"https://example.com/photo.jpg""""))
    }

    @Test
    fun `createItem fails fast when accessToken missing`() = runBlocking {
        val engine = MockEngine { error("should not be called") }
        val noTokenMarketplace = Marketplace(name = "No Token", type = "MercadoLibre")

        val result = MercadoLibreItemCreationEngine(engine).createItem(noTokenMarketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("accessToken") == true)
    }

    @Test
    fun `createItem reports 401 without retrying`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"invalid_token"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("401") == true)
    }

    @Test
    fun `createItem surfaces field-level cause errors on validation failure`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"invalid attributes","cause":[{"code":"item.attributes.missing","message":"Missing attribute BRAND"}]}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("invalid attributes") == true)
        assertTrue(result.msgError?.contains("Missing attribute BRAND") == true)
    }

    @Test
    fun `createItem falls back to raw body when error JSON has neither message nor error field`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"status":500,"cause":[]}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("500") == true)
        assertTrue(result.msgError?.contains("""{"status":500,"cause":[]}""") == true)
    }

    @Test
    fun `createItem preserves HTTP status when error body is not JSON`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "<html><body>502 Bad Gateway</body></html>",
                status = HttpStatusCode.BadGateway,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("502") == true)
        assertTrue(result.msgError?.contains("Bad Gateway") == true)
    }

    @Test
    fun `createItem reports missing item id in an otherwise successful response`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"status":"active"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibreItemCreationEngine(engine).createItem(marketplace, payload)

        assertTrue(!result.isOk)
        assertTrue(result.msgError?.contains("no item id") == true)
    }
}
