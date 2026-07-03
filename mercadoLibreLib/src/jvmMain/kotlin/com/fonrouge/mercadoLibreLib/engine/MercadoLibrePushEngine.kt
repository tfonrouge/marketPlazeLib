package com.fonrouge.mercadoLibreLib.engine

import com.fonrouge.base.state.SimpleState
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.IMercadoLibrePushService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*

/**
 * PUT /items/{id} against the global MercadoLibre API base (api.mercadolibre.com, not
 * .com.mx — see mppErsaPack LEDGER D8/MarketPlace(MODULE) for why the regional host was rejected).
 *
 * Does not refresh expired tokens — `marketplace.connectionConfig["accessToken"]` must already
 * be valid. Callers that also own OAuth refresh (today: mppErsaPack's MercadoLibreApiClient)
 * are responsible for retrying after a refresh on 401.
 *
 * [testEngine] lets tests inject a mocked [io.ktor.client.engine.mock.MockEngine] instead of the
 * real CIO engine; when null (production default) a CIO client is built and closed per call.
 */
class MercadoLibrePushEngine(private val testEngine: io.ktor.client.engine.HttpClientEngine? = null) : IMercadoLibrePushService {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun buildHttpClient(): HttpClient {
        val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(jsonParser)
            }
        }
        return testEngine?.let { HttpClient(it, config) } ?: HttpClient(CIO, config)
    }

    /** Shared PUT /items/{id} plumbing — auth, error extraction, client lifecycle. */
    private suspend fun putItemField(marketplace: Marketplace, itemId: String, body: JsonObject, successMsg: String): SimpleState {
        val accessToken = marketplace.connectionConfig["accessToken"]
            ?: return SimpleState(isOk = false, msgError = "Missing accessToken in connectionConfig")

        val client = buildHttpClient()
        try {
            val response = client.put("$API_BASE/items/$itemId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }

            if (response.status.value == 401) {
                return SimpleState(isOk = false, msgError = "401 Unauthorized updating $itemId — access token expired, refresh required")
            }
            if (!response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val message = try {
                    val errorBody = jsonParser.parseToJsonElement(bodyText).jsonObject
                    errorBody["message"]?.jsonPrimitive?.contentOrNull
                        ?: errorBody["error"]?.jsonPrimitive?.contentOrNull
                        ?: bodyText.take(200)
                } catch (e: Exception) {
                    // ML sometimes returns a non-JSON error body (HTML gateway page on 502/503).
                    // Fall back to the raw body rather than losing the HTTP status to a parse exception.
                    bodyText.take(200)
                }
                return SimpleState(isOk = false, msgError = "MercadoLibre update failed for $itemId: ${response.status} — $message")
            }

            return SimpleState(isOk = true, msgOk = successMsg)
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "MercadoLibre update error for $itemId: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun updateAvailableQuantity(marketplace: Marketplace, itemId: String, quantity: Int): SimpleState =
        putItemField(
            marketplace,
            itemId,
            buildJsonObject { put("available_quantity", quantity) },
            "Updated $itemId to available_quantity=$quantity",
        )

    override suspend fun updateSellerCustomField(marketplace: Marketplace, itemId: String, sellerCustomField: String): SimpleState =
        putItemField(
            marketplace,
            itemId,
            buildJsonObject { put("seller_custom_field", sellerCustomField) },
            "Updated $itemId to seller_custom_field=$sellerCustomField",
        )

    companion object {
        private const val API_BASE = "https://api.mercadolibre.com"
    }
}
