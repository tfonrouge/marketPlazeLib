package com.fonrouge.shopifyLib.colls

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.toJsonString
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.OId
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import com.fonrouge.shopifyLib.model.IStore
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

abstract class IShopifyColl<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>, UID : Any, QQ : Query<D>, D : Query.Data>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, T, ID, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
) {
    companion object {
        val syncingElements: HashMap<OId<*>, Boolean> = HashMap()
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }
        }
        var storeColl: IStoreColl<*, *, *, *>? = null
        private const val API_VERSION = "2025-10" // Use a stable Shopify Admin API version
    }

    abstract val queryListKClass: KClass<QQ>

    abstract val shopifyIgnoredFields: Set<String>
    abstract val shopifyIdName: String?
    suspend fun buildApolloClient(storeId: OId<IStore>): ItemState<ApolloClient> {
        val store = storeColl?.findById(storeId) ?: return ItemState(isOk = false, msgError = "Store Coll not defined")
        val a = ApolloClient.Builder()
            .serverUrl(shopifyEndpoint(store.url))
            .httpHeaders(
                listOf(
                    HttpHeader("X-Shopify-Access-Token", store.accessToken)
                )
            ).build()
        return ItemState(a)
    }

    protected fun graphQLFieldNames(): String =
        commonContainer.itemKClass.memberProperties.mapNotNull { kProperty ->
            if (kProperty.name == shopifyIdName || kProperty.name in shopifyIgnoredFields) return@mapNotNull null
            if (kProperty.name == "_id") {
                shopifyIdName?.let { "_id: $it" }
            } else {
                val shopifyField = kProperty.findAnnotation<ShopifyField>()
                if (shopifyField?.ignore == true) return@mapNotNull null
                val name = shopifyField?.name
                if (name.isNullOrBlank()) kProperty.name else name
            }
        }.filterNot { it == "_id" || it == "storeId" }
            .joinToString("\n") {
                "${" ".repeat(12)}$it"
            }

    private fun shopifyEndpoint(shopDomain: String) =
        "$shopDomain/admin/api/$API_VERSION/graphql.json".let {
            if (it.startsWith("https://")) it else "https://$it"
        }

    suspend fun syncShopifyToLocal(storeId: OId<IStore>): SimpleState {
        val itemState = buildApolloClient(storeId)
        val apolloClient: ApolloClient =
            itemState.item ?: return SimpleState(isOk = false, msgError = itemState.msgError)
        val queryList = queryListKClass.constructors.first().call(100)
        val response: ApolloResponse<D> = apolloClient.query(queryList).execute()
        val rawString = response.data?.let {
            queryList.adapter().toJsonString(it)
        } ?: return simpleErrorState("No data")
        val document = Json.parseToJsonElement(rawString) as JsonObject
        println("rawString = $rawString, document = $document")
        return SimpleState(isOk = true)
    }
}
