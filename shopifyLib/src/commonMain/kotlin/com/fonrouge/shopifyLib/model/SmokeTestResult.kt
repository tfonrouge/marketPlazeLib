package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class SmokeTestResult(
    val productId: String,
    val fieldTested: String,
    val originalValue: String?,
    val testValue: String?,
    val updateVerified: Boolean,
    val revertVerified: Boolean,
    val error: String? = null,
)
