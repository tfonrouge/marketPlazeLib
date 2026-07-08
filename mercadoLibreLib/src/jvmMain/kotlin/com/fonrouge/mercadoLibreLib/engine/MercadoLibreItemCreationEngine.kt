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
        // La API actual de ML crea el item por "familia": exige `family_name` y RECHAZA `title`
        // ("The fields [title] are invalid") — verificado en vivo (mppErsaPack LEDGER D70, dos
        // iteraciones: sin family_name → "family_name requerido"; con title+family_name → "title
        // inválido"). Para un item sin variaciones ML deriva el título de `family_name`, así que el
        // título editado por el operador va acá y NO se manda `title`.
        put("family_name", payload.title)
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
                    // ML's validation errors for POST /items list field-level failures under `cause`.
                    // Depending on the error kind the detail lives in different keys: `message` for
                    // required/attribute errors, `code`/`references`/`department` for `invalid_fields`.
                    // Render each cause entry as compactly-but-completely as possible so the operator
                    // sees WHICH field ML rejected — falling back to the raw entry when unstructured.
                    val cause = errorBody["cause"]?.jsonArray?.mapNotNull { entry ->
                        val obj = entry.jsonObject
                        val code = obj["code"]?.jsonPrimitive?.contentOrNull
                        val msg = obj["message"]?.jsonPrimitive?.contentOrNull
                        val refs = obj["references"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                            ?.takeIf { it.isNotEmpty() }?.joinToString(",")
                        val parts = listOfNotNull(code, msg, refs?.let { "[$it]" }).filter { it.isNotBlank() }
                        if (parts.isNotEmpty()) parts.joinToString(" ") else obj.toString()
                    }?.filter { it.isNotBlank() } ?: emptyList()
                    val base = errorBody["message"]?.jsonPrimitive?.contentOrNull
                        ?: errorBody["error"]?.jsonPrimitive?.contentOrNull
                        ?: bodyText.take(200)
                    // If cause parsing yielded nothing useful, include the raw body so no detail is lost.
                    when {
                        cause.isNotEmpty() -> "$base — ${cause.joinToString("; ")}"
                        else -> "$base — ${bodyText.take(400)}"
                    }
                } catch (e: Exception) {
                    // ML sometimes returns a non-JSON error body (HTML gateway page on 502/503).
                    bodyText.take(400)
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
