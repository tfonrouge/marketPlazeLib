package com.fonrouge.shopifyLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.OId
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.fullStack.mongoDb.toBsonSet
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import com.fonrouge.shopifyLib.model.*
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.litote.kmongo.eq
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

abstract class IShopifyColl<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>, UID : Any, S : IStore, ED : IElementsData<T>>(
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
        private const val API_VERSION = "2025-07" // Use a stable Shopify Admin API version
    }

    abstract val shopifyIgnoredFields: Set<String>
    abstract val shopifyIdName: String?

    protected fun graphQLfieldNames(): String =
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

    abstract val elementQuery: String?
    abstract val elementSerializer: KSerializer<ED>
    abstract val storeCollFun: () -> IStoreColl<*, S, *, *>

    @OptIn(InternalSerializationApi::class)
    suspend fun graphQLRequest(
        store: IStore,
        elementQuery: String,
        first: Int = 250,
        query: String? = null,
        after: String? = null
    ): GraphQLResponse<ED> {
        val request = GraphQLRequest(
            query = elementQuery,
            variables = ElementsVars(first = first, query = query, after = after)
        )
        val response = httpClient.post(urlString = shopifyEndpoint(store.url)) {
            header("X-Shopify-Access-Token", store.accessToken)
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.OK -> Json.decodeFromString(
                deserializer = GraphQLResponse.serializer(elementSerializer),
                string = response.bodyAsText()
            )

            else -> GraphQLResponse(
                errorCode = response.status.value,
                errors = listOf(GraphQLError(errorCode = response.status.value, message = response.status.description))
            )
        }
    }

    @Suppress("unused")
    suspend fun syncShopifyToLocal(
        storeId: OId<IStore>,
    ): SimpleState {
        val elementQuery = elementQuery ?: return SimpleState(isOk = false, msgError = "No elementQuery defined")
        if (syncingElements[storeId] == true) return SimpleState(isOk = false, msgError = "Syncing products...")
        val store = storeCollFun().findById(storeId) ?: return SimpleState(isOk = false, msgError = "Store not found")
        try {
            syncingElements[storeId] = true
            var cursor: String? = null
            var hasNext = true
            val updateOneModels = mutableListOf<UpdateOneModel<T>>()
            while (hasNext) {
                val result: GraphQLResponse<ED> = graphQLRequest(
                    store = store,
                    elementQuery = elementQuery,
                    first = 250,
                    query = null,     // e.g., "status:active"
                    after = cursor
                )
                if (!result.errors.isNullOrEmpty()) {
                    val messages = result.errors.joinToString("; ") { "${it.errorCode ?: ""}: ${it.message}" }
                    syncingElements[storeId] = false
                    return simpleErrorState("Shopify GraphQL errors: $messages")
                }
                val connection = result.data?.elements ?: break
                connection.edges.forEach { edge: ElementEdge<T> ->
                    val t: T = edge.node
                    updateOneModels += UpdateOneModel(
                        BaseDoc<*>::_id eq t._id,
                        toBsonSet(t),
                        UpdateOptions().upsert(true)
                    )
                    println("x = $t")
                }
                hasNext = connection.pageInfo.hasNextPage
                cursor = connection.pageInfo.endCursor
            }
            if (updateOneModels.isNotEmpty()) {
                val r = coroutine.bulkWrite(updateOneModels)
                println("bulkWrite result = $r")
            }
            syncingElements[storeId] = false
        } catch (e: Exception) {
            System.err.println("syncShopifyToLocal exception = $e")
            return simpleErrorState("syncShopifyToLocal exception = $e")
        } finally {
            syncingElements[storeId] = false
        }
        return SimpleState(isOk = true)
    }
}
