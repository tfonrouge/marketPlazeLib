package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class ElementConnection<T>(
    val edges: List<ElementEdge<T>>,
    val pageInfo: PageInfo
)