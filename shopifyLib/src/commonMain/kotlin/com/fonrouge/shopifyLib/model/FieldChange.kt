package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class FieldChange(
    val fieldName: String,
    val oldValue: String? = null,
    val newValue: String? = null,
)
