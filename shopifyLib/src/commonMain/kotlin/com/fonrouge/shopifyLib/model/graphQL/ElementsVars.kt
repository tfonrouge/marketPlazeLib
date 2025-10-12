package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class ElementsVars(
    val first: Int,
    val query: String? = null,
    val after: String? = null,
)