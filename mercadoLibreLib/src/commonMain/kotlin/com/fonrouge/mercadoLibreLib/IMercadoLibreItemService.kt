package com.fonrouge.mercadoLibreLib

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCreateItemResult
import com.fonrouge.mercadoLibreLib.model.MercadoLibreItemPayload

/**
 * `POST /items` — creates a new real listing on MercadoLibre, visible to buyers. Separate from
 * [IMercadoLibrePushService] (PUT, updates an existing item) and [IMercadoLibreCategoryService]
 * (GET, category discovery) — a third distinct responsibility (D65's one-service-one-responsibility
 * precedent): this is the only service in the create-item flow that performs a write.
 */
interface IMercadoLibreItemService {
    suspend fun createItem(marketplace: Marketplace, payload: MercadoLibreItemPayload): MercadoLibreCreateItemResult
}
