package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class ElementEdge<T>(
    val cursor: String,
    val node: T
)