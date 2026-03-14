package com.fonrouge.shopifyLib.colls

import com.fonrouge.GetProductsQuery
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.shopifyLib.common.CommonShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProduct
import com.fonrouge.shopifyLib.model.ShopifyProductFilter
import io.ktor.server.application.*
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import com.fonrouge.fullStack.mongoDb.ResultUnit
import org.litote.kmongo.eq
import kotlin.reflect.KClass

@Suppress("unused")
abstract class IShopifyProductColl<UID : Any>(
    debug: Boolean = false,
) : IShopifyColl<CommonShopifyProduct, ShopifyProduct, StringId<ShopifyProduct>, ShopifyProductFilter, UID, GetProductsQuery, GetProductsQuery.Data>(
    commonContainer = CommonShopifyProduct,
    debug = debug,
) {
    final override val queryListKClass = GetProductsQuery::class
    final override val shopifyIgnoredFields: Set<String> = setOf(ShopifyProduct::marketplaceId.name)
    final override val shopifyIdName: String = "id"
    override val subNodes: Map<String, Pair<String?, KClass<*>>> = mapOf(
        "media" to ("image" to ShopifyProduct.Image::class)
    )

    override fun additionalFields(marketplaceId: MarketplaceId, document: Document): Document {
        return document.append(ShopifyProduct::marketplaceId.name, marketplaceId.id)
    }

    override suspend fun CoroutineCollection<ShopifyProduct>.indexes() {
        ensureUniqueIndex(ShopifyProduct::handle)
    }

    override fun matchStage(call: ApplicationCall?, apiFilter: ShopifyProductFilter, resultUnit: ResultUnit): Bson? {
        val matchDoc = mutableListOf<Bson>()
        matchDoc += ShopifyProduct::marketplaceId eq apiFilter.masterItemId
        return and(matchDoc)
    }
}
