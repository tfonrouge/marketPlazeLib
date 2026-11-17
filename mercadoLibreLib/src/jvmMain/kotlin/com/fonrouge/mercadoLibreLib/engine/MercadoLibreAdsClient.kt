package com.fonrouge.mercadoLibreLib.engine

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsProduct
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsRequest
import com.fonrouge.mercadoLibreLib.model.MercadoLibreAdsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Cliente de la API de Mercado Ads (PublicidadMercadoAds B2 S1; B1 R14 y R15). Sólo lee.
 * - **Sólo `GET`**, a rutas de la lista blanca bajo `/advertising/` ([isAllowedPath]); no tiene ningún otro verbo.
 * - **Nunca refresca el token** (MarketPlace D42): usa `marketplace.connectionConfig["accessToken"]` tal como está y no
 *   conoce `/oauth`. Un 401 o 403 se devuelve como corte para que el ciclo se detenga; rotar el token es del order-sync.
 * - **El token nunca sale**: no va en la URL, y se borra de cualquier texto de error antes de devolverlo.
 * - Traduce el estado HTTP según S1: 2xx con JSON → `Ok`; 401, 403, 429, 400 y cualquier otro 4xx → `Cut`;
 *   5xx, tiempo agotado o cuerpo ilegible → `Retryable`; 404 en `advertisers` → `NotEnabled`.
 *
 * Las URL y los parámetros repiten los de la sonda que leyó producción el 2026-11-11 (L-019, L-020).
 * [testEngine] permite probar con `MockEngine`; en producción es `null` y se usa CIO.
 */
class MercadoLibreAdsClient(
    private val testEngine: HttpClientEngine? = null,
    private val requestTimeoutMillis: Long = 60_000,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Una solicitud de lectura de un día de Product Ads del anunciante [advertiserId]. */
    suspend fun get(marketplace: Marketplace, advertiserId: Long, request: MercadoLibreAdsRequest): MercadoLibreAdsResponse {
        val date = request.date
        return when (request) {
            is MercadoLibreAdsRequest.AdGroupsPage -> call(
                marketplace, "/advertising/$SITE/advertisers/$advertiserId/product_ads/ad_groups/search", API_V2,
                listOf("date_from" to date, "date_to" to date, "metrics" to METRICS_UPPER, "metrics_summary" to "true", "limit" to "50") +
                    (if (request.offset > 0) listOf("offset" to request.offset.toString()) else emptyList()),
            )
            is MercadoLibreAdsRequest.CampaignsDaily -> call(
                marketplace, "/advertising/$SITE/advertisers/$advertiserId/product_ads/campaigns/search", API_V2,
                listOf("date_from" to date, "date_to" to date, "metrics" to METRICS_LOWER, "aggregation_type" to "DAILY", "limit" to "50"),
            )
            is MercadoLibreAdsRequest.AdGroupAds -> call(
                marketplace, "/advertising/$SITE/product_ads/ad_groups/${request.adGroupId}/ads", API_V2,
                listOf("date_from" to date, "date_to" to date, "metrics" to METRICS_LOWER) +
                    (if (request.offset > 0) listOf("offset" to request.offset.toString()) else emptyList()),
            )
        }
    }

    /** Anunciante de [product] (`Api-Version: 1`). Medido: PADS da 2303516; DISPLAY y BADS dan 404 → `NotEnabled`. */
    suspend fun advertisers(marketplace: Marketplace, product: MercadoLibreAdsProduct): MercadoLibreAdsResponse =
        call(marketplace, "/advertising/advertisers", API_V1, listOf("product_id" to product.name), notFoundIsNotEnabled = true)

    /** Bonificaciones de publicidad de la cuenta (`Api-Version: 1`). */
    suspend fun bonifications(marketplace: Marketplace): MercadoLibreAdsResponse =
        call(marketplace, "/advertising/advertisers/bonifications", API_V1, emptyList())

    private suspend fun call(
        marketplace: Marketplace,
        path: String,
        apiVersion: String,
        params: List<Pair<String, String>>,
        notFoundIsNotEnabled: Boolean = false,
    ): MercadoLibreAdsResponse {
        require(isAllowedPath(path)) { "ruta fuera de la lista blanca: $path" }
        val token = marketplace.connectionConfig["accessToken"]?.takeIf { it.isNotBlank() }
            ?: return MercadoLibreAdsResponse.Cut(0, "sin accessToken en el Marketplace")
        val url = API_BASE + path + params.joinToString("&", prefix = if (params.isEmpty()) "" else "?") { (k, v) ->
            // La coma va literal, como en la sonda que leyó producción (`safe='[],'`).
            "${k.encodeURLParameter()}=${v.encodeURLParameter().replace("%2C", ",")}"
        }
        val client = testEngine?.let { HttpClient(it) { install(HttpTimeout) { requestTimeoutMillis = this@MercadoLibreAdsClient.requestTimeoutMillis } } }
            ?: HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = this@MercadoLibreAdsClient.requestTimeoutMillis } }
        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Api-Version", apiVersion)
            }
            val status = response.status.value
            val text = response.bodyAsText()
            when {
                status in 200..299 -> {
                    val body = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                    if (body != null) MercadoLibreAdsResponse.Ok(body)
                    else MercadoLibreAdsResponse.Retryable("cuerpo ilegible con estado $status en $path")
                }
                status == 404 && notFoundIsNotEnabled -> MercadoLibreAdsResponse.NotEnabled
                status in 400..499 -> MercadoLibreAdsResponse.Cut(status, "estado $status en $path: ${text.take(200)}".withoutSecret(token))
                else -> MercadoLibreAdsResponse.Retryable("estado $status en $path")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MercadoLibreAdsResponse.Retryable("${e::class.simpleName}: ${e.message}".withoutSecret(token))
        } finally {
            client.close()
        }
    }

    companion object {
        private const val API_BASE = "https://api.mercadolibre.com"
        private const val SITE = "MLM"
        private const val API_V1 = "1"
        private const val API_V2 = "2"
        private const val METRICS_LOWER =
            "clicks,prints,cost,cpc,ctr,direct_amount,indirect_amount,total_amount,direct_units_quantity,indirect_units_quantity,units_quantity"
        private val METRICS_UPPER = METRICS_LOWER.uppercase()

        /** Lista blanca de la sonda (L-019): anunciantes y bonificaciones, y Product Ads del sitio. Nada más. */
        fun isAllowedPath(path: String): Boolean =
            !path.contains("..") && !path.contains("oauth") && (
                path == "/advertising/advertisers" ||
                    path == "/advertising/advertisers/bonifications" ||
                    path.startsWith("/advertising/$SITE/advertisers/") ||
                    path.startsWith("/advertising/$SITE/product_ads/")
                )

        private fun String.withoutSecret(secret: String): String = if (secret.isEmpty()) this else replace(secret, "***")
    }
}
