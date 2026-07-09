package com.fonrouge.mercadoLibreLib

import com.fonrouge.base.state.SimpleState
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.mercadoLibreLib.model.MercadoLibreItemStatus

interface IMercadoLibrePushService {
    suspend fun updateAvailableQuantity(marketplace: Marketplace, itemId: String, quantity: Int): SimpleState

    /**
     * Sets the listing's lifecycle [status] on ML (`PUT /items/{id} {"status": ...}`) — the control
     * side of MarketPlace(MODULE) 11d (pause/close/reactivate a listing from the app instead of the
     * ML web UI). Callers should refresh the local mirror after a successful change so the app
     * reflects the new status at once (`mercadoLibreProduct` is the single source of truth, D71).
     */
    suspend fun updateItemStatus(marketplace: Marketplace, itemId: String, status: MercadoLibreItemStatus): SimpleState

    /**
     * Sets ML's own "SKU vendedor" (`seller_custom_field`) on a listing — the field this library's
     * pull side reads back as [com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct.sellerSku].
     * Used to curate/backfill the SKU↔MercadoLibre-item mapping so future syncs (and callers of
     * [updateAvailableQuantity]) can match by SKU without re-curating by hand each time.
     */
    suspend fun updateSellerCustomField(marketplace: Marketplace, itemId: String, sellerCustomField: String): SimpleState
}
