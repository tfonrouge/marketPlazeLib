package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc

/**
 * Defines a product listed in a specific marketplace, identified by a unique [IMarketProductId].
 *
 * This interface represents the blueprint for market products, providing the basic structure
 * for handling marketplace-related functionality. Products that extend this interface are expected
 * to be identified by a unique key and may include publish state information.
 *
 * @param ID The type of the identifier for the market product, which must extend [IMarketProductId].
 * @property publishEnabled Indicates whether the product is enabled for publishing in the marketplace.
 *                          A value of `true` means it is enabled, `false` means it is disabled, and
 *                          `null` indicates that the state is not defined.
 */
interface IMarketProduct<ID : IMarketProductId<*>> : BaseDoc<ID> {
    val publishEnabled: Boolean?
}
