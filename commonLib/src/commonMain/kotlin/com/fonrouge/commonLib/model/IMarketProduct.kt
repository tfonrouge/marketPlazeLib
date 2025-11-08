package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc

interface IMarketProduct<PID : Any> : BaseDoc<MarketProductId<PID>> {
    val publishEnabled: Boolean?
}
