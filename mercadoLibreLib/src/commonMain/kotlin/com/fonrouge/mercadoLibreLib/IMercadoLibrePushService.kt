package com.fonrouge.mercadoLibreLib

import com.fonrouge.base.state.SimpleState
import com.fonrouge.commonLib.model.Marketplace

interface IMercadoLibrePushService {
    suspend fun updateAvailableQuantity(marketplace: Marketplace, itemId: String, quantity: Int): SimpleState

    /**
     * Sets ML's own "SKU vendedor" (`seller_custom_field`) on a listing — the field this library's
     * pull side reads back as [com.fonrouge.mercadoLibreLib.model.MercadoLibreProduct.sellerSku].
     * Used to curate/backfill the SKU↔MercadoLibre-item mapping so future syncs (and callers of
     * [updateAvailableQuantity]) can match by SKU without re-curating by hand each time.
     */
    suspend fun updateSellerCustomField(marketplace: Marketplace, itemId: String, sellerCustomField: String): SimpleState
}
