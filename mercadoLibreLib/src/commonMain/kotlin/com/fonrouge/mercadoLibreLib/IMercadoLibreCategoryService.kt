package com.fonrouge.mercadoLibreLib

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategoryAttribute
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategorySettings
import com.fonrouge.mercadoLibreLib.model.MercadoLibreCategorySuggestion

/**
 * Read-only category discovery for publishing new items — separate from [IMercadoLibrePushService]
 * (which only ever PUTs an existing item's inventory/SKU) because this is GET-only and belongs to
 * the create-item flow, not the update-existing-item flow.
 */
interface IMercadoLibreCategoryService {
    /** `GET /sites/{siteId}/domain_discovery/search` — never treat the top result as authoritative. */
    suspend fun suggestCategories(marketplace: Marketplace, query: String): List<MercadoLibreCategorySuggestion>

    /** `GET /categories/{categoryId}/attributes`. */
    suspend fun getCategoryAttributes(marketplace: Marketplace, categoryId: String): List<MercadoLibreCategoryAttribute>

    /** `GET /categories/{categoryId}` (`.settings` only). */
    suspend fun getCategorySettings(marketplace: Marketplace, categoryId: String): MercadoLibreCategorySettings
}
