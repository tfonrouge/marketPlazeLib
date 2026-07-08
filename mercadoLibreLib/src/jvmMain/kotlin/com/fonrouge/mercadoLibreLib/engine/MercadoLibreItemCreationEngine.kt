package com.fonrouge.mercadoLibreLib.engine

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.IMercadoLibreItemService
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCreateItemResult
import com.fonrouge.mercadoLibreLib.model.MercadoLibreItemPayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*

/**
 * `POST /items` against the same global API base as [MercadoLibrePushEngine]/[MercadoLibreCategoryEngine]
 * (mppErsaPack LEDGER D8). Creates a REAL, live listing visible to buyers — not reversible the way a
 * local DB write is (LEDGER D62/D68: explicitly gated behind explicit write authorization before any
 * live test).
 *
 * [testEngine] lets tests inject a mocked [io.ktor.client.engine.mock.MockEngine], same convention as
 * the other two engines in this package.
 */
class MercadoLibreItemCreationEngine(private val testEngine: io.ktor.client.engine.HttpClientEngine? = null) : IMercadoLibreItemService {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun buildHttpClient(): HttpClient {
        val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(jsonParser)
            }
        }
        return testEngine?.let { HttpClient(it, config) } ?: HttpClient(CIO, config)
    }

    private fun buildBody(payload: MercadoLibreItemPayload): JsonObject = buildJsonObject {
        put("title", payload.title)
        put("category_id", payload.categoryId)
        put("price", payload.price)
        put("currency_id", payload.currencyId)
        put("available_quantity", payload.availableQuantity)
        put("buying_mode", payload.buyingMode)
        put("condition", payload.condition)
        put("listing_type_id", payload.listingTypeId)
        putJsonArray("attributes") {
            payload.attributes.forEach { (id, valueName) ->
                addJsonObject {
                    put("id", id)
                    put("value_name", valueName)
                }
            }
        }
        if (payload.pictureUrls.isNotEmpty()) {
            putJsonArray("pictures") {
                payload.pictureUrls.forEach { url ->
                    addJsonObject { put("source", url) }
                }
            }
        }
        payload.description?.takeIf { it.isNotBlank() }?.let {
            put("description", buildJsonObject { put("plain_text", it) })
        }
    }

    override suspend fun createItem(marketplace: Marketplace, payload: MercadoLibreItemPayload): MercadoLibreCreateItemResult {
        val accessToken = marketplace.connectionConfig["accessToken"]
            ?: return MercadoLibreCreateItemResult(isOk = false, msgError = "Missing accessToken in connectionConfig")

        val client = buildHttpClient()
        try {
            val response = client.post("$API_BASE/items") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(buildBody(payload).toString())
            }

            val bodyText = response.bodyAsText()
            if (response.status.value == 401) {
                return MercadoLibreCreateItemResult(isOk = false, msgError = "401 Unauthorized creating item — access token expired, refresh required")
            }
            if (!response.status.isSuccess()) {
                val message = try {
                    val errorBody = jsonParser.parseToJsonElement(bodyText).jsonObject
                    // ML's validation errors for POST /items commonly list field-level failures under
                    // `cause` (e.g. a missing required attribute) — surface them alongside the base message.
                    val cause = errorBody["cause"]?.jsonArray?.mapNotNull {
                        it.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                    }?.filter { it.isNotBlank() } ?: emptyList()
                    val base = errorBody["message"]?.jsonPrimitive?.contentOrNull
                        ?: errorBody["error"]?.jsonPrimitive?.contentOrNull
                        ?: bodyText.take(200)
                    if (cause.isNotEmpty()) "$base — ${cause.joinToString("; ")}" else base
                } catch (e: Exception) {
                    // ML sometimes returns a non-JSON error body (HTML gateway page on 502/503).
                    bodyText.take(200)
                }
                return MercadoLibreCreateItemResult(isOk = false, msgError = "MercadoLibre item creation failed: ${response.status} — $message")
            }

            val created = jsonParser.parseToJsonElement(bodyText).jsonObject
            val itemId = created["id"]?.jsonPrimitive?.contentOrNull
                ?: return MercadoLibreCreateItemResult(isOk = false, msgError = "MercadoLibre creation succeeded but response had no item id: ${bodyText.take(200)}")
            return MercadoLibreCreateItemResult(
                isOk = true,
                itemId = itemId,
                permalink = created["permalink"]?.jsonPrimitive?.contentOrNull,
            )
        } catch (e: Exception) {
            return MercadoLibreCreateItemResult(isOk = false, msgError = "MercadoLibre item creation error: ${e.message}")
        } finally {
            client.close()
        }
    }

    companion object {
        private const val API_BASE = "https://api.mercadolibre.com"
    }
}
