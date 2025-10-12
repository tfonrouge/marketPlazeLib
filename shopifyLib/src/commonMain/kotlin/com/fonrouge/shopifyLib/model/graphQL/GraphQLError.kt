package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLError(
    val errorCode: Int? = null,
    val message: String
)