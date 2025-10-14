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
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.state.simpleErrorState
import com.fonrouge.base.types.OId
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.shopifyLib.model.IStore
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
) {
    companion object {
        var storeColl: IStoreColl<*, *, *, *>? = null
        private const val API_VERSION = "2025-10" // Use a stable Shopify Admin API version
    }

    abstract val queryListKClass: KClass<QQ>
    abstract val shopifyIgnoredFields: Set<String>
    abstract val shopifyIdName: String?
    open val subNodes: Map<String, Pair<String?, KClass<*>>> = mapOf()

    /**
     * Adds additional fields to a given document for a specific Shopify store.
     *
     * @param storeId The unique identifier of the Shopify store. Represents an instance of `IStore` using its object ID.
     * @param document The original document to which additional fields should be appended.
     * @return The updated document with additional fields appended.
     */
    open fun additionalFields(storeId: OId<IStore>, document: Document): Document = document

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

    private fun shopifyEndpoint(shopDomain: String) =
        "$shopDomain/admin/api/$API_VERSION/graphql.json".let {
            if (it.startsWith("https://")) it else "https://$it"
        }

    /**
     * Synchronizes data from a Shopify store to a local database.
     *
     * @param storeId The identifier of the Shopify store to be synchronized. Represents an instance of `IStore` using its unique object ID.
     * @return A `SimpleState` object indicating the success or failure of the synchronization process, along with optional messages.
     */
    @Suppress("unused")
    @OptIn(InternalSerializationApi::class)
    suspend fun syncShopifyToLocal(storeId: OId<IStore>): SimpleState {
        val itemState = try {
            buildApolloClient(storeId)
        } catch (e: Exception) {
            return simpleErrorState("buildApolloClient exception: ${e.message}")
        }
        val apolloClient: ApolloClient =
            itemState.item ?: return SimpleState(isOk = false, msgError = itemState.msgError)
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
                        storeId = storeId,
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
}
