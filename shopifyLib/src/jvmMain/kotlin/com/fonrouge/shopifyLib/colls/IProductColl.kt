package com.fonrouge.shopifyLib.colls

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.http.HttpHeader
import com.fonrouge.GetProductDetailQuery
import com.fonrouge.GetProductsQuery
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.OId
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.common.ICommonProduct
import com.fonrouge.shopifyLib.model.IStore
import com.fonrouge.shopifyLib.model.graphQL.ElementConnection
import com.fonrouge.shopifyLib.model.graphQL.IElementsData
import com.fonrouge.shopifyLib.model.product.ShopifyProduct
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.WriteModel
import io.ktor.server.application.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.conversions.Bson
import org.litote.kmongo.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

abstract class IProductColl<CC : ICommonProduct<FILT>, FILT : IApiFilter<*>, UID : Any, S : IStore>(
    commonContainer: CC,
    debug: Boolean = false,
) : IShopifyColl<CC, ShopifyProduct, StringId<ShopifyProduct>, FILT, UID, S, IProductColl.ProductsData>(
    commonContainer = commonContainer,
    debug = debug,
) {
    final override val shopifyIgnoredFields: Set<String> = setOf(ShopifyProduct::storeId.name)
    final override val shopifyIdName: String = "id"

    @Serializable
    data class ProductsData(
        @SerialName("products")
        override val elements: ElementConnection<ShopifyProduct>
    ) : IElementsData<ShopifyProduct>

    final override val elementQuery: String by lazy {
        $$"""
    query Products($first: Int!, $query: String, $after: String) {
      product(first: $first, query: $query, after: $after) {
        pageInfo {
          hasNextPage
          endCursor
        }
        edges {
          cursor
          node {
$${graphQLfieldNames()}
          }
        }
      }
    }
""".trimIndent()
    }

    @OptIn(InternalSerializationApi::class)
    final override val elementSerializer: KSerializer<ProductsData> = ProductsData.serializer()

    val apolloClient
        get() = ApolloClient.Builder()
            .serverUrl("https://casa-dulce-usa.myshopify.com/admin/api/2025-10/graphql.json")
            .httpHeaders(
                listOf(
                    HttpHeader("X-Shopify-Access-Token", "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")
                )
            )
            .build()

    suspend fun fetchProduct(id: StringId<ShopifyProduct>): SimpleState {
        val response: ApolloResponse<GetProductDetailQuery.Data> =
            apolloClient.query(GetProductDetailQuery(id.id)).execute()
        println("r = $response")
        return if (response.data != null) {
            val x = response.data?.product
            println("x = $x")
//            response.data?.products?.edges?.forEach { edge: AllProductsQuery.Edge ->
//                println("handle = ${edge.node.handle} title = ${edge.node.title}")
//            }
            SimpleState(isOk = true)
        } else {
            println("response.errors = ${response.errors}")
            SimpleState(isOk = false, msgError = response.errors?.get(0)?.message ?: "")
        }
    }

    val properties: Collection<KProperty1<GetProductsQuery.Node, *>> = GetProductsQuery.Node::class.memberProperties

    suspend fun fetchProducts(storeId: OId<IStore>): SimpleState {
        val response = apolloClient.query(GetProductsQuery(100)).execute()
        val updateOneModels = mutableListOf<WriteModel<ShopifyProduct>>()
        response.data?.products?.edges?.forEach { edge: GetProductsQuery.Edge ->
            val product: GetProductsQuery.Node = edge.node
            val setToList = mutableListOf<SetTo<Any?>>()
            setToList += ShopifyProduct::storeId setTo storeId
            properties.forEach { property ->
                val key = property.name
                val value = property.get(product)
                val x = property setTo value
                setToList += x
                println("$key = $value")
            }
            updateOneModels += UpdateOneModel(
                ShopifyProduct::_id eq StringId(product._id),
                set(*setToList.toTypedArray()),
                UpdateOptions().upsert(true)
            )
            println("handle = ${product.handle} title = ${product.title}")
        }
        if (updateOneModels.isNotEmpty()) {
            println("updateOneModels = ${updateOneModels.size}")
            coroutine.bulkWrite(updateOneModels)
        }
        return SimpleState(isOk = true)
    }

    override fun matchStage(call: ApplicationCall?, apiFilter: FILT, resultUnit: ResultUnit): Bson? {
        val matchDoc = mutableListOf<Bson>()
        matchDoc += ShopifyProduct::storeId eq apiFilter.masterItemId
        return and(matchDoc)
    }
}
