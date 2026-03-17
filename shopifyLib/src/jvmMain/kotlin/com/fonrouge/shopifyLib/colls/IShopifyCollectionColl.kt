package com.fonrouge.shopifyLib.colls

import com.fonrouge.GetCollectionsQuery
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.fullStack.mongoDb.ResultUnit
import com.fonrouge.shopifyLib.common.CommonShopifyCollection
import com.fonrouge.shopifyLib.model.ShopifyCollection
import com.fonrouge.shopifyLib.model.ShopifyCollectionFilter
import io.ktor.server.application.*
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

@Suppress("unused")
abstract class IShopifyCollectionColl<UID : Any>(
    debug: Boolean = false,
) : IShopifyColl<ShopifyCollection, StringId<ShopifyCollection>, ShopifyCollectionFilter, UID, GetCollectionsQuery, GetCollectionsQuery.Data>(
    commonContainer = CommonShopifyCollection,
    debug = debug,
) {
    final override val queryListKClass = GetCollectionsQuery::class
    final override val shopifyIgnoredFields: Set<String> = setOf(ShopifyCollection::marketplaceId.name)
    final override val shopifyIdName: String = "id"

    override fun additionalFields(marketplaceId: MarketplaceId, document: Document): Document {
        return document.append(ShopifyCollection::marketplaceId.name, marketplaceId.id)
    }

    override suspend fun CoroutineCollection<ShopifyCollection>.indexes() {
        ensureUniqueIndex(ShopifyCollection::handle)
    }

    override fun matchStage(call: ApplicationCall?, apiFilter: ShopifyCollectionFilter, resultUnit: ResultUnit): Bson? {
        val matchDoc = mutableListOf<Bson>()
        matchDoc += ShopifyCollection::marketplaceId eq apiFilter.masterItemId
        return and(matchDoc)
    }
}
