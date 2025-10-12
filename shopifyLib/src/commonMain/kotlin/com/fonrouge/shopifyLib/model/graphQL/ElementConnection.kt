package com.fonrouge.shopifyLib.model.graphQL

import com.fonrouge.shopifyLib.model.graphQL.ElementEdge
import com.fonrouge.shopifyLib.model.graphQL.PageInfo
import kotlinx.serialization.Serializable

@Serializable
data class ElementConnection<T>(
    val edges: List<ElementEdge<T>>,
    val pageInfo: PageInfo
)