package com.fonrouge.shopifyLib.colls

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.types.OId
import com.fonrouge.fullStack.mongoDb.Coll
import com.fonrouge.shopifyLib.colls.IShopifyColl.Companion.storeColl
import com.fonrouge.shopifyLib.common.ICommonStore
import com.fonrouge.shopifyLib.model.IStore

abstract class IStoreColl<CC : ICommonStore<S, FILT>, S : IStore, FILT : IApiFilter<*>, UID : Any>(
    commonContainer: CC,
    debug: Boolean = false,
) : Coll<CC, S, OId<IStore>, FILT, UID>(
    commonContainer = commonContainer,
    debug = debug,
) {
    init {
        storeColl = this
    }
}
