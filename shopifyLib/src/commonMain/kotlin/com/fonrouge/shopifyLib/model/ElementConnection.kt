package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class ElementConnection<T>(
    val edges: List<ElementEdge<T>>,
    val pageInfo: PageInfo
)
