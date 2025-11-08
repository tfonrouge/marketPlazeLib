package com.fonrouge.shopifyLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.shopifyLib.colls.IShopifyColl.Companion.storeColl
import com.fonrouge.shopifyLib.common.ICommonStore
import com.fonrouge.shopifyLib.model.IStore
import com.fonrouge.shopifyLib.model.IStoreId

abstract class IStoreColl<CC : ICommonStore<S, FILT>, S : IStore, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, S, IStoreId, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
) {
    init {
        storeColl = this
    }
}
