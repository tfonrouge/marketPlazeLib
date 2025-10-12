package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLRequest<V>(
    val query: String,
    val variables: V
)