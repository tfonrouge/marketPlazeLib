package com.fonrouge.commonLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.commonLib.model.IMarketProduct
import com.fonrouge.commonLib.model.MarketProductId
import com.fonrouge.fullStack.mongoDb.Coll

/**
 * Abstract class representing a collection of market products, designed to handle operations
 * for objects implementing the [IMarketProduct] interface, identified by [MarketProductId].
 *
 * @param CC The type of the common container extending [ICommonContainer], used to manage the
 *           collection of market products.
 * @param T The type of the market product implementing [IMarketProduct].
 * @param PID The type of the product ID associated with the market product.
 * @param FILT The type of API filter used for querying or filtering the collection.
 * @param UID The type of the unique identifier for user-related functionalities or operations.
 * @constructor Initializes the market product collection with a common container and an optional debug flag.
 * @property commonContainer Represents the container used to hold and manage the collection of market products.
 * @property debug When set to `true`, enables detailed debugging logs for the collection's operations.
 */
@Suppress("unused")
abstract class IMarketProductColl<CC : ICommonContainer<T, MarketProductId<PID>, FILT>, T : IMarketProduct<PID>, PID : Any, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, T, MarketProductId<PID>, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
)
