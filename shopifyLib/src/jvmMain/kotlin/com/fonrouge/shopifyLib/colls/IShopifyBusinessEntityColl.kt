package com.fonrouge.shopifyLib.colls

import com.fonrouge.GetBusinessEntitiesQuery
import com.fonrouge.base.api.ApiFilter
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.shopifyLib.common.CommonShopifyBusinessEntity
import com.fonrouge.shopifyLib.model.ShopifyBusinessEntity
import kotlin.reflect.KClass

abstract class IShopifyBusinessEntityColl<UID : Any>() :
    IShopifyColl<CommonShopifyBusinessEntity, ShopifyBusinessEntity, StringId<ShopifyBusinessEntity>, ApiFilter, UID, GetBusinessEntitiesQuery, GetBusinessEntitiesQuery.Data>(
        commonContainer = CommonShopifyBusinessEntity,
    ) {
    final override val queryListKClass: KClass<GetBusinessEntitiesQuery> = GetBusinessEntitiesQuery::class
    final override val shopifyIdName: String = "id"

    @Suppress("unused")
    suspend fun ping(marketplace: Marketplace): SimpleState {
        val apolloClient = buildApolloClient(marketplace)
        val apolloResponse = apolloClient.query(GetBusinessEntitiesQuery()).execute()
        apolloResponse.exception?.let { return SimpleState(isOk = false, msgError = it.message ?: "Unknown error") }
        val businessEntity: GetBusinessEntitiesQuery.BusinessEntity =
            apolloResponse.data?.businessEntities?.firstOrNull() ?: return SimpleState(
                isOk = false,
                msgError = "No business found"
            )
        return SimpleState(
            isOk = true,
            msgOk = "Display Name: ${businessEntity.displayName}, Company Name: ${businessEntity.companyName}, Primary: ${businessEntity.primary}"
        )
    }
}
