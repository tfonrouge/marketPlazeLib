package com.fonrouge.shopifyLib

import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.Product
import com.fonrouge.shopifyLib.model.ShopifyCollection
import com.fonrouge.shopifyLib.model.ShopifyProduct

interface IShopifyPushService {
    suspend fun createProduct(marketplace: Marketplace, product: Product): SimpleState
    suspend fun updateProduct(marketplace: Marketplace, product: Product, shopifyId: String): SimpleState
    suspend fun deleteProduct(marketplace: Marketplace, shopifyId: String): SimpleState
    suspend fun updateInventory(marketplace: Marketplace, shopifyId: String, locationId: String, quantity: Int): SimpleState
    suspend fun setPublishState(marketplace: Marketplace, shopifyId: String, published: Boolean): SimpleState
    suspend fun addToCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String): SimpleState
    suspend fun removeFromCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String): SimpleState
    suspend fun uploadImage(marketplace: Marketplace, shopifyId: String, imageUrl: String, altText: String?): SimpleState
    suspend fun deleteImage(marketplace: Marketplace, shopifyId: String, imageId: String): SimpleState
    suspend fun reorderImages(marketplace: Marketplace, shopifyId: String, imageIds: List<String>): SimpleState
    suspend fun createCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState
    suspend fun updateCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState
    suspend fun deleteCollection(marketplace: Marketplace, collectionId: String): SimpleState
    suspend fun uploadImageFromBytes(marketplace: Marketplace, shopifyId: String, imageBytes: ByteArray, filename: String, altText: String? = null): SimpleState
    suspend fun getProduct(marketplace: Marketplace, shopifyId: String): ItemState<ShopifyProduct>
}
