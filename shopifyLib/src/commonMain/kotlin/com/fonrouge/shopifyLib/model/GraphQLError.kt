package com.fonrouge.shopifyLib.model

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLError(
    val message: String
)
