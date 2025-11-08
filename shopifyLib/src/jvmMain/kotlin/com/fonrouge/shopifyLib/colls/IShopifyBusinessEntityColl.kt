package com.fonrouge.shopifyLib.colls

import com.fonrouge.GetBusinessEntitiesQuery
import com.fonrouge.base.api.ApiFilter
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.shopifyLib.common.CommonShopifyBusinessEntity
import com.fonrouge.shopifyLib.model.IStoreId
import com.fonrouge.shopifyLib.model.ShopifyBusinessEntity
import kotlin.reflect.KClass

abstract class IShopifyBusinessEntityColl<UID : Any>() :
    IShopifyColl<CommonShopifyBusinessEntity, ShopifyBusinessEntity, StringId<ShopifyBusinessEntity>, ApiFilter, UID, GetBusinessEntitiesQuery, GetBusinessEntitiesQuery.Data>(
        commonContainer = CommonShopifyBusinessEntity,
    ) {
    final override val queryListKClass: KClass<GetBusinessEntitiesQuery> = GetBusinessEntitiesQuery::class
    final override val shopifyIdName: String = "id"

    /**
     * Sends a GraphQL query to fetch business entities associated with the given store.
     * Builds an Apollo client for the specified store and executes the query.
     * If an exception occurs or no business entity is found, the method returns an error state.
     * Otherwise, it returns a success state with details of the primary business entity.
     *
     * @param iStoreId The unique identifier of the Shopify store for which to fetch business entities.
     * @return A `SimpleState` instance representing the outcome of the operation. Contains success or error state and corresponding message.
     */
    @Suppress("unused")
    suspend fun ping(iStoreId: IStoreId): SimpleState {
        val apolloClient = buildApolloClient(iStoreId)
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
