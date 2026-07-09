import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.engine.MercadoLibrePushEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MercadoLibrePushEngineTest {

    private val marketplace = Marketplace(
        name = "Test MercadoLibre",
        type = "MercadoLibre",
        connectionConfig = mapOf("accessToken" to "APP_USR-token"),
    )

    @Test
    fun `updateAvailableQuantity sends PUT with bearer token and available_quantity body`() = runBlocking {
        var capturedUrl = ""
        var capturedAuth = ""
        var capturedBody = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization] ?: ""
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"id":"MLM123","available_quantity":10}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(marketplace, "MLM123", 10)

        assertTrue(!result.hasError, "expected success, got: ${result.msgError}")
        assertEquals("https://api.mercadolibre.com/items/MLM123", capturedUrl)
        assertEquals("Bearer APP_USR-token", capturedAuth)
        assertEquals("""{"available_quantity":10}""", capturedBody)
    }

    @Test
    fun `updateAvailableQuantity reports 401 without retrying`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"invalid_token","error":"not_found"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(marketplace, "MLM123", 10)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("401") == true)
    }

    @Test
    fun `updateAvailableQuantity surfaces MercadoLibre error message on failure`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"item does not admit stock modification","error":"bad_request","status":400}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(marketplace, "MLM123", 10)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("item does not admit stock modification") == true)
    }

    @Test
    fun `updateAvailableQuantity fails fast when accessToken missing`() = runBlocking {
        val engine = MockEngine { error("should not be called") }
        val noTokenMarketplace = Marketplace(name = "No Token", type = "MercadoLibre")

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(noTokenMarketplace, "MLM123", 10)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("accessToken") == true)
    }

    @Test
    fun `updateAvailableQuantity falls back to raw body when error JSON has neither message nor error field`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"status":500,"cause":[]}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(marketplace, "MLM123", 10)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("500") == true)
        assertTrue(result.msgError?.contains("""{"status":500,"cause":[]}""") == true)
    }

    @Test
    fun `updateAvailableQuantity preserves HTTP status when error body is not JSON`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "<html><body>502 Bad Gateway</body></html>",
                status = HttpStatusCode.BadGateway,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateAvailableQuantity(marketplace, "MLM123", 10)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("502") == true, "expected the HTTP status to survive a non-JSON body, got: ${result.msgError}")
        assertTrue(result.msgError?.contains("Bad Gateway") == true, "expected the raw body to surface, got: ${result.msgError}")
    }

    @Test
    fun `updateSellerCustomField sends PUT with bearer token and seller_custom_field body`() = runBlocking {
        var capturedUrl = ""
        var capturedAuth = ""
        var capturedBody = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization] ?: ""
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"id":"MLM123","seller_custom_field":"EP-V22"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateSellerCustomField(marketplace, "MLM123", "EP-V22")

        assertTrue(!result.hasError, "expected success, got: ${result.msgError}")
        assertEquals("https://api.mercadolibre.com/items/MLM123", capturedUrl)
        assertEquals("Bearer APP_USR-token", capturedAuth)
        assertEquals("""{"seller_custom_field":"EP-V22"}""", capturedBody)
    }

    @Test
    fun `updateSellerCustomField surfaces MercadoLibre error message on failure`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"seller_custom_field already in use","error":"conflict"}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine).updateSellerCustomField(marketplace, "MLM123", "EP-V22")

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("seller_custom_field already in use") == true)
    }

    @Test
    fun `updateItemStatus sends PUT with status apiValue body (11d control)`() = runBlocking {
        var capturedUrl = ""
        var capturedBody = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"id":"MLM123","status":"paused"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine)
            .updateItemStatus(marketplace, "MLM123", com.fonrouge.mercadoLibreLib.model.MercadoLibreItemStatus.PAUSED)

        assertTrue(!result.hasError, "expected success, got: ${result.msgError}")
        assertEquals("https://api.mercadolibre.com/items/MLM123", capturedUrl)
        // Sends ML's lowercase apiValue, not the enum name.
        assertEquals("""{"status":"paused"}""", capturedBody)
    }

    @Test
    fun `updateItemStatus surfaces MercadoLibre error message on failure`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"message":"item cannot be closed","error":"bad_request"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = MercadoLibrePushEngine(engine)
            .updateItemStatus(marketplace, "MLM123", com.fonrouge.mercadoLibreLib.model.MercadoLibreItemStatus.CLOSED)

        assertTrue(result.hasError)
        assertTrue(result.msgError?.contains("item cannot be closed") == true)
    }
}
