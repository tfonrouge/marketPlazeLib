package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class CollectionRule(
    val column: String,
    val relation: String,
    val condition: String,
)
