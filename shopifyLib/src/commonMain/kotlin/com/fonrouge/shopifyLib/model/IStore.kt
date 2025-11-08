package com.fonrouge.shopifyLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias IStoreId = OId<IStore>

@Serializable
abstract class IStore : BaseDoc<IStoreId> {
    abstract val name: String
    abstract val url: String
    abstract val apiVersion: String
    abstract val accessToken: String
    abstract val byteArrayImage: String?
    abstract val enabled: Boolean
    abstract val totalOnEachStore: Boolean
}
