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
    val referralKitCost: Money = Money.ZERO,
    val variants: List<ProductVariant> = emptyList(),
    val images: List<ProductImage> = emptyList(),
    val presentations: List<ProductPresentation> = emptyList(),
    // ShopifyPushSync enrichment
    val erpSourceId: String? = null,
    val priceLists: List<PriceListEntry> = emptyList(),
    val inventoryQuantity: Int = 0,
    val descriptionHtml: String? = null,
    val publishEnabled: Boolean = false,
    val status: String = "draft",
    val tags: List<String> = emptyList(),
    val weight: Double? = null,
    val weightUnit: String? = null,
    val barcode: String? = null,
    val seoTitle: String? = null,
    val seoDescription: String? = null,
) : BaseDoc<ProductId>
