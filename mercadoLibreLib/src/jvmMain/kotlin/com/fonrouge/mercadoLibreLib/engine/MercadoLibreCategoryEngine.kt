package com.fonrouge.mercadoLibreLib.engine

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.IMercadoLibreCategoryService
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategoryAttribute
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategorySettings
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategorySuggestion
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*

/**
 * Read-only category discovery against the same global API base as [MercadoLibrePushEngine]
 * (mppErsaPack LEDGER D8). Never refreshes tokens — same convention as the push engine.
 *
 * All three calls throw on failure (unlike [MercadoLibrePushEngine], which returns [SimpleState])
 * because callers here need a typed result to render a form, not a pass/fail — the caller decides
 * how to surface a failure (mppErsaPack wraps these in try/catch, same as its other ML calls).
 */
class MercadoLibreCategoryEngine(private val testEngine: io.ktor.client.engine.HttpClientEngine? = null) : IMercadoLibreCategoryService {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun buildHttpClient(): HttpClient {
        val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(jsonParser)
            }
        }
        return testEngine?.let { HttpClient(it, config) } ?: HttpClient(CIO, config)
    }

    private suspend fun get(marketplace: Marketplace, path: String, block: (io.ktor.client.request.HttpRequestBuilder.() -> Unit)? = null): JsonElement {
        val accessToken = marketplace.connectionConfig["accessToken"]
            ?: error("Missing accessToken in connectionConfig")
        val client = buildHttpClient()
        try {
            val response = client.get("$API_BASE$path") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                block?.invoke(this)
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val message = try {
                    jsonParser.parseToJsonElement(bodyText).jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: bodyText.take(200)
                } catch (e: Exception) {
                    bodyText.take(200)
                }
                error("MercadoLibre GET $path failed: ${response.status} — $message")
            }
            return jsonParser.parseToJsonElement(bodyText)
        } finally {
            client.close()
        }
    }

    override suspend fun suggestCategories(marketplace: Marketplace, query: String): List<MercadoLibreCategorySuggestion> {
        val siteId = marketplace.connectionConfig["site"] ?: "MLM"
        val result = get(marketplace, "/sites/$siteId/domain_discovery/search") {
            parameter("q", query)
            parameter("limit", 3)
        }
        return result.jsonArray.map { entry ->
            val obj = entry.jsonObject
            MercadoLibreCategorySuggestion(
                categoryId = obj["category_id"]!!.jsonPrimitive.content,
                categoryName = obj["category_name"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        }
    }

    override suspend fun getCategoryAttributes(marketplace: Marketplace, categoryId: String): List<MercadoLibreCategoryAttribute> {
        val result = get(marketplace, "/categories/$categoryId/attributes")
        return result.jsonArray.map { entry ->
            val obj = entry.jsonObject
            val tags = obj["tags"]?.jsonObject
            MercadoLibreCategoryAttribute(
                id = obj["id"]!!.jsonPrimitive.content,
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                valueType = obj["value_type"]?.jsonPrimitive?.contentOrNull ?: "string",
                required = tags?.get("required")?.jsonPrimitive?.booleanOrNull ?: false,
                catalogRequired = tags?.get("catalog_required")?.jsonPrimitive?.booleanOrNull ?: false,
                values = obj["values"]?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull } ?: emptyList(),
            )
        }
    }

    override suspend fun getCategorySettings(marketplace: Marketplace, categoryId: String): MercadoLibreCategorySettings {
        val result = get(marketplace, "/categories/$categoryId").jsonObject
        val settings = result["settings"]?.jsonObject
        return MercadoLibreCategorySettings(
            maxTitleLength = settings?.get("max_title_length")?.jsonPrimitive?.intOrNull,
            maxPicturesPerItem = settings?.get("max_pictures_per_item")?.jsonPrimitive?.intOrNull,
            minimumPrice = settings?.get("minimum_price")?.jsonPrimitive?.doubleOrNull,
            minimumPriceCurrency = settings?.get("minimum_price_currency")?.jsonPrimitive?.contentOrNull,
            itemConditions = settings?.get("item_conditions")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
            catalogDomain = settings?.get("catalog_domain")?.jsonPrimitive?.contentOrNull,
        )
    }

    companion object {
        private const val API_BASE = "https://api.mercadolibre.com"
    }
}
