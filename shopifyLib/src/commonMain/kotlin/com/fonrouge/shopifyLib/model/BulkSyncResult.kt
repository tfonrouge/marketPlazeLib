package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class BulkSyncResult(
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val errors: List<SyncError> = emptyList(),
)
