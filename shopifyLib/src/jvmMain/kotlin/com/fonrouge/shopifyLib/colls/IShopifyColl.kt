package com.fonrouge.shopifyLib.colls

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.toJsonString
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.bson.toBsonSet
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.fullStack.mongoDb.Coll
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.bson.Document
import org.litote.kmongo.eq
import kotlin.reflect.KClass

abstract class IShopifyColl<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>, UID : Any, QQ : Query<D>, D : Query.Data>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, T, ID, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
), IMarketplaceSync<T> {

    abstract val queryListKClass: KClass<QQ>
    open val shopifyIgnoredFields: Set<String> = emptySet()
    abstract val shopifyIdName: String?
    open val subNodes: Map<String, Pair<String?, KClass<*>>> = mapOf()

    open fun additionalFields(marketplaceId: MarketplaceId, document: Document): Document = document

    fun buildApolloClient(marketplace: Marketplace): ApolloClient {
        val config = marketplace.connectionConfig
        val url = config["url"] ?: throw Exception("Missing 'url' in marketplace connection config")
        val apiVersion = config["apiVersion"] ?: throw Exception("Missing 'apiVersion' in marketplace connection config")
        val accessToken = config["accessToken"] ?: throw Exception("Missing 'accessToken' in marketplace connection config")
        val apolloClient = ApolloClient.Builder()
            .serverUrl(shopifyEndpoint(url, apiVersion))
            .httpHeaders(
                listOf(
                    HttpHeader("X-Shopify-Access-Token", accessToken)
                )
            ).build()
        return apolloClient
    }

    private fun shopifyEndpoint(shopDomain: String, apiVersion: String) =
        "$shopDomain/admin/api/$apiVersion/graphql.json".let {
            if (it.startsWith("https://")) it else "https://$it"
        }

    fun processSubNodes(document: Document): Document {
        subNodes.forEach { (key, pair) ->
            document.get(key, Document::class.java)?.let {
                val subDocuments = mutableListOf<Document>()
                it.getList("edges", Document::class.java)?.forEach { edge ->
                    edge.get("node", Document::class.java)?.let { node ->
                        subDocuments += if (pair.first == null) {
                            node
                        } else {
                            node.get(pair.first, Document::class.java)
                        }
                    }
                }
                document[key] = subDocuments
            }
        }
        return document
    }

    @Suppress("unused")
    @OptIn(InternalSerializationApi::class)
    suspend fun syncShopifyToLocal(marketplace: Marketplace): SimpleState {
        val apolloClient = try {
            buildApolloClient(marketplace)
        } catch (e: Exception) {
            return simpleErrorState("buildApolloClient exception: ${e.message}")
        }
        var hasNextPage = true
        var endCursor: String? = null
        val updateOneModels = mutableListOf<UpdateOneModel<T>>()
        val initTime = System.currentTimeMillis()
        while (hasNextPage) {
            val queryList = queryListKClass.constructors.first().call(250, Optional.present(endCursor))
            val response: ApolloResponse<D> = apolloClient.query(queryList).execute()
            val rawString = response.data?.let {
                queryList.adapter().toJsonString(it)
            } ?: return simpleErrorState("No data found")
            val document = Document.parse(rawString)
            val edges = document.keys.firstOrNull()?.let { document[it] as? Document }
            val pageInfo = edges?.get("pageInfo", Document::class.java)
            endCursor = pageInfo?.getString("endCursor")
            hasNextPage = (pageInfo?.getBoolean("hasNextPage") == true) && endCursor.isNullOrEmpty().not()
            edges?.getList("edges", Document::class.java)?.forEach { edge ->
                val nodeJson = processSubNodes(
                    document = additionalFields(
                        marketplaceId = marketplace._id,
                        document = edge.get("node", Document::class.java)
                    )
                )
                val item: T = Json.decodeFromString(commonContainer.itemKClass.serializer(), nodeJson.toJson())
                updateOneModels += UpdateOneModel(
                    BaseDoc<ID>::_id eq item._id,
                    item.toBsonSet(),
                    UpdateOptions().upsert(true)
                )
            }
        }
        val useTime = System.currentTimeMillis() - initTime
        return if (updateOneModels.isNotEmpty()) {
            val bulkWriteResult = coroutine.bulkWrite(updateOneModels)
            if (bulkWriteResult.wasAcknowledged()) {
                SimpleState(isOk = true, "Updated ${bulkWriteResult.modifiedCount} documents, $useTime ms used")
            } else {
                SimpleState(isOk = false, "Bulk write operation was not acknowledged")
            }
        } else {
            SimpleState(isOk = true, "No updates found, $useTime ms used")
        }
    }

    override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
        return syncShopifyToLocal(marketplace)
    }
}
