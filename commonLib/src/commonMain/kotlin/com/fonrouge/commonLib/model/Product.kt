package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias ProductId = OId<Product>

@Serializable
data class Product(
    override val _id: ProductId = OId(),
    val sku: String,
    val name: String,
    val baseCost: Money = Money.ZERO,
    val variants: List<ProductVariant> = emptyList(),
    val images: List<ProductImage> = emptyList(),
    val presentations: List<ProductPresentation> = emptyList(),
) : BaseDoc<ProductId>
