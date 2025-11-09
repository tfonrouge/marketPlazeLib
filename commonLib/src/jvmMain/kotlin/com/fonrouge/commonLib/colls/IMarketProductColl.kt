package com.fonrouge.commonLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.IMarketProduct
import com.fonrouge.commonLib.model.IMarketProductId
import com.fonrouge.fullStack.mongoDb.Coll

/**
 * Abstract class representing a collection of marketplace products.
 *
 * This class serves as a base for handling collections of products specific to various marketplaces,
 * providing functionality to manage entities that conform to the [IMarketProduct] interface and
 * leveraging filtering and identification mechanisms through generics.
 *
 * @param CC The type of the common container used in the collection, extending [ICommonContainer].
 *           It provides the structure for operations like retrieval, filtering, and management
 *           of marketplace products.
 * @param T The type representing the market products within the collection, which must implement
 *          the [IMarketProduct] interface.
 * @param ID The type of the unique identifier used by the market products, extending [IMarketProductId].
 * @param FILT The type of the API filter mechanism used to filter and query the collection.
 * @param UID The type representing a unique user or session-related identifier that may be
 *            associated with operations on the collection.
 * @param commonContainer The common container instance used by the collection for underlying
 *                        operations and data access.
 * @param debug A flag indicating whether debug mode is enabled for the collection. When `true`,
 *              additional debug information may be provided for diagnostics or testing purposes.
 */
@Suppress("unused")
abstract class IMarketProductColl<CC : ICommonContainer<T, ID, FILT>, T : IMarketProduct<ID>, ID : IMarketProductId<*>, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, T, ID, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
)
