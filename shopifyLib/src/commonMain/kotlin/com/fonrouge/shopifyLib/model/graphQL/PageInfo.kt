package com.fonrouge.shopifyLib.model.graphQL

import kotlinx.serialization.Serializable

@Serializable
data class PageInfo(
    val hasNextPage: Boolean,
    val endCursor: String? = null
)