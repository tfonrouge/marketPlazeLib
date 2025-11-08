package com.fonrouge.shopifyLib.colls

import com.fonrouge.GetProductsQuery
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.common.ICommonShopifyProduct
import com.fonrouge.shopifyLib.model.IStoreId
import com.fonrouge.shopifyLib.model.ShopifyProduct
import io.ktor.server.application.*
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import kotlin.reflect.KClass

/**
 * Abstract class representing a specialized collection for Shopify products.
 * This class handles interactions with Shopify products, facilitating operations
 * such as querying, filtering, and syncing product data between Shopify and a local store.
 *
 * @param CC Type parameter representing the common product container class.
 * @param FILT Type parameter representing the API filter used for product filtering.
 * @param UID Type parameter representing the unique identifier for an operation.
 * @property queryListKClass Represents the class reference for the Shopify GraphQL query used to fetch products.
 * @property shopifyIgnoredFields A set of field names that should be ignored when syncing or mapping Shopify product data.
 * @property shopifyIdName The name of the unique identifier field in Shopify for products.
 */
@Suppress("unused")
abstract class IShopifyProductColl<CC : ICommonShopifyProduct<FILT>, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : IShopifyColl<CC, ShopifyProduct, StringId<ShopifyProduct>, FILT, UID, GetProductsQuery, GetProductsQuery.Data>(
    commonContainer = commonContainer,
    debug = debug,
) {
    final override val queryListKClass = GetProductsQuery::class
    final override val shopifyIgnoredFields: Set<String> = setOf(ShopifyProduct::storeId.name)
    final override val shopifyIdName: String = "id"
    override val subNodes: Map<String, Pair<String?, KClass<*>>> = mapOf(
        "media" to ("image" to ShopifyProduct.Image::class)
    )

    override fun additionalFields(storeId: IStoreId, document: Document): Document {
        return document.append(ShopifyProduct::storeId.name, storeId.id)
    }

    override suspend fun CoroutineCollection<ShopifyProduct>.indexes() {
        ensureUniqueIndex(ShopifyProduct::handle)
    }

    override fun matchStage(call: ApplicationCall?, apiFilter: FILT, resultUnit: ResultUnit): Bson? {
        val matchDoc = mutableListOf<Bson>()
        matchDoc += ShopifyProduct::storeId eq apiFilter.masterItemId
        return and(matchDoc)
    }
}
