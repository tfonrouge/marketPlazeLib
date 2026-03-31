package com.fonrouge.shopifyLib.engine

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.http.HttpHeader
import com.fonrouge.CollectionAddProductsMutation
import com.fonrouge.CollectionRemoveProductsMutation
import com.fonrouge.GetProductInventoryItemsQuery
import com.fonrouge.InventorySetQuantitiesMutation
import com.fonrouge.ProductCreateMediaMutation
import com.fonrouge.ProductCreateMutation
import com.fonrouge.ProductDeleteMediaMutation
import com.fonrouge.ProductDeleteMutation
import com.fonrouge.ProductReorderMediaMutation
import com.fonrouge.ProductUpdateMutation
import com.fonrouge.PublishablePublishMutation
import com.fonrouge.PublishableUnpublishMutation
import com.fonrouge.CollectionCreateMutation
import com.fonrouge.CollectionDeleteMutation
import com.fonrouge.CollectionUpdateMutation
import com.fonrouge.StagedUploadsCreateMutation
import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.Product
import com.fonrouge.shopifyLib.IShopifyPushService
import com.fonrouge.shopifyLib.model.ShopifyCollection
import com.fonrouge.type.CollectionDeleteInput
import com.fonrouge.type.CollectionInput
import com.fonrouge.type.CollectionRuleColumn
import com.fonrouge.type.CollectionRuleInput
import com.fonrouge.type.CollectionRuleRelation
import com.fonrouge.type.CollectionRuleSetInput
import com.fonrouge.type.StagedUploadInput
import com.fonrouge.type.StagedUploadTargetGenerateUploadResource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import com.fonrouge.type.CreateMediaInput
import com.fonrouge.type.InventoryQuantityInput
import com.fonrouge.type.InventorySetQuantitiesInput
import com.fonrouge.type.MediaContentType
import com.fonrouge.type.MoveInput
import com.fonrouge.type.ProductCreateInput
import com.fonrouge.type.ProductDeleteInput
import com.fonrouge.type.ProductStatus
import com.fonrouge.type.ProductUpdateInput
import com.fonrouge.type.PublicationInput
import com.fonrouge.type.SEOInput

class ShopifyPushEngine : IShopifyPushService {

    private fun buildApolloClient(marketplace: Marketplace): ApolloClient {
        val config = marketplace.connectionConfig
        val url = config["url"] ?: throw Exception("Missing 'url' in marketplace connection config")
        val apiVersion = config["apiVersion"] ?: throw Exception("Missing 'apiVersion' in marketplace connection config")
        val accessToken = config["accessToken"] ?: throw Exception("Missing 'accessToken' in marketplace connection config")
        val shopifyEndpoint = "$url/admin/api/$apiVersion/graphql.json".let {
            if (it.startsWith("https://")) it else "https://$it"
        }
        return ApolloClient.Builder()
            .serverUrl(shopifyEndpoint)
            .httpHeaders(listOf(HttpHeader("X-Shopify-Access-Token", accessToken)))
            .build()
    }

    private fun Product.toProductStatus(): ProductStatus = when (status) {
        "active" -> ProductStatus.ACTIVE
        "archived" -> ProductStatus.ARCHIVED
        else -> ProductStatus.DRAFT
    }

    private fun Product.toSeoInput(): Optional<SEOInput?> {
        if (seoTitle == null && seoDescription == null) return Optional.Absent
        return Optional.present(
            SEOInput(
                title = Optional.presentIfNotNull(seoTitle),
                description = Optional.presentIfNotNull(seoDescription),
            )
        )
    }

    override suspend fun createProduct(marketplace: Marketplace, product: Product): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = ProductCreateMutation(
                product = Optional.present(
                    ProductCreateInput(
                        title = Optional.present(product.name),
                        descriptionHtml = Optional.presentIfNotNull(product.descriptionHtml),
                        tags = Optional.present(product.tags),
                        status = Optional.present(product.toProductStatus()),
                        seo = product.toSeoInput(),
                    )
                )
            )
            val response = client.mutation(mutation).execute()
            val data = response.data?.productCreate
                ?: return SimpleState(isOk = false, msgError = "No response data from productCreate")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            val shopifyId = data.product?.id
                ?: return SimpleState(isOk = false, msgError = "productCreate returned no product id")
            return SimpleState(isOk = true, msgOk = shopifyId)
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "productCreate error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun updateProduct(marketplace: Marketplace, product: Product, shopifyId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = ProductUpdateMutation(
                product = Optional.present(
                    ProductUpdateInput(
                        id = Optional.present(shopifyId),
                        title = Optional.present(product.name),
                        descriptionHtml = Optional.presentIfNotNull(product.descriptionHtml),
                        tags = Optional.present(product.tags),
                        status = Optional.present(product.toProductStatus()),
                        seo = product.toSeoInput(),
                    )
                )
            )
            val response = client.mutation(mutation).execute()
            val data = response.data?.productUpdate
                ?: return SimpleState(isOk = false, msgError = "No response data from productUpdate")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Updated $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "productUpdate error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun deleteProduct(marketplace: Marketplace, shopifyId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = ProductDeleteMutation(input = ProductDeleteInput(id = shopifyId))
            val response = client.mutation(mutation).execute()
            val data = response.data?.productDelete
                ?: return SimpleState(isOk = false, msgError = "No response data from productDelete")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Deleted ${data.deletedProductId}")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "productDelete error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun updateInventory(marketplace: Marketplace, shopifyId: String, locationId: String, quantity: Int): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            // Step 1: Query inventoryItemId from product's first variant
            val variantsResponse = client.query(GetProductInventoryItemsQuery(productId = shopifyId)).execute()
            val variant = variantsResponse.data?.product?.variants?.edges?.firstOrNull()?.node
                ?: return SimpleState(isOk = false, msgError = "No variants found for product $shopifyId")
            val inventoryItemId = variant.inventoryItem.id

            // Step 2: Set quantity via inventorySetQuantities
            val mutation = InventorySetQuantitiesMutation(
                input = InventorySetQuantitiesInput(
                    reason = "correction",
                    name = "available",
                    quantities = listOf(
                        InventoryQuantityInput(
                            inventoryItemId = inventoryItemId,
                            locationId = locationId,
                            quantity = quantity,
                        )
                    ),
                )
            )
            val response = client.mutation(mutation).execute()
            val data = response.data?.inventorySetQuantities
                ?: return SimpleState(isOk = false, msgError = "No response data from inventorySetQuantities")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Inventory set to $quantity for $inventoryItemId at $locationId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "updateInventory error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun setPublishState(marketplace: Marketplace, shopifyId: String, published: Boolean): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val publicationInput = listOf(PublicationInput())
            if (published) {
                val response = client.mutation(PublishablePublishMutation(id = shopifyId, input = publicationInput)).execute()
                val data = response.data?.publishablePublish
                    ?: return SimpleState(isOk = false, msgError = "No response data from publishablePublish")
                if (data.userErrors.isNotEmpty()) {
                    return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
                }
            } else {
                val response = client.mutation(PublishableUnpublishMutation(id = shopifyId, input = publicationInput)).execute()
                val data = response.data?.publishableUnpublish
                    ?: return SimpleState(isOk = false, msgError = "No response data from publishableUnpublish")
                if (data.userErrors.isNotEmpty()) {
                    return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
                }
            }
            return SimpleState(isOk = true, msgOk = if (published) "Published $shopifyId" else "Unpublished $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "setPublishState error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun addToCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = CollectionAddProductsMutation(id = collectionId.id, productIds = listOf(shopifyProductId))
            val response = client.mutation(mutation).execute()
            val data = response.data?.collectionAddProducts
                ?: return SimpleState(isOk = false, msgError = "No response data from collectionAddProducts")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Added to collection ${collectionId.id}")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "addToCollection error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun removeFromCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = CollectionRemoveProductsMutation(id = collectionId.id, productIds = listOf(shopifyProductId))
            val response = client.mutation(mutation).execute()
            val data = response.data?.collectionRemoveProducts
                ?: return SimpleState(isOk = false, msgError = "No response data from collectionRemoveProducts")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Removed from collection ${collectionId.id}")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "removeFromCollection error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun uploadImage(marketplace: Marketplace, shopifyId: String, imageUrl: String, altText: String?): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            // Use originalSource URL directly via productCreateMedia (no staged upload needed for URLs)
            val mutation = ProductCreateMediaMutation(
                productId = shopifyId,
                media = listOf(
                    CreateMediaInput(
                        originalSource = imageUrl,
                        alt = Optional.presentIfNotNull(altText),
                        mediaContentType = MediaContentType.IMAGE,
                    )
                )
            )
            val response = client.mutation(mutation).execute()
            val data = response.data?.productCreateMedia
                ?: return SimpleState(isOk = false, msgError = "No response data from productCreateMedia")
            if (data.mediaUserErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.mediaUserErrors.joinToString { it.message })
            }
            val mediaId = data.media?.firstOrNull()?.onMediaImage?.id ?: "unknown"
            return SimpleState(isOk = true, msgOk = "Uploaded image $mediaId to $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "uploadImage error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun deleteImage(marketplace: Marketplace, shopifyId: String, imageId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = ProductDeleteMediaMutation(productId = shopifyId, mediaIds = listOf(imageId))
            val response = client.mutation(mutation).execute()
            val data = response.data?.productDeleteMedia
                ?: return SimpleState(isOk = false, msgError = "No response data from productDeleteMedia")
            if (data.mediaUserErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.mediaUserErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Deleted media $imageId from $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "deleteImage error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun reorderImages(marketplace: Marketplace, shopifyId: String, imageIds: List<String>): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val moves = imageIds.mapIndexed { index, id -> MoveInput(id = id, newPosition = "$index") }
            val mutation = ProductReorderMediaMutation(id = shopifyId, moves = moves)
            val response = client.mutation(mutation).execute()
            val data = response.data?.productReorderMedia
                ?: return SimpleState(isOk = false, msgError = "No response data from productReorderMedia")
            if (data.mediaUserErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.mediaUserErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Reordered ${imageIds.size} media for $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "reorderImages error: ${e.message}")
        } finally {
            client.close()
        }
    }

    private fun ShopifyCollection.toCollectionInput(): CollectionInput {
        val ruleInputs = rules.map { rule ->
            CollectionRuleInput(
                column = CollectionRuleColumn.safeValueOf(rule.column),
                relation = CollectionRuleRelation.safeValueOf(rule.relation),
                condition = rule.condition,
            )
        }
        return CollectionInput(
            id = Optional.presentIfNotNull(_id.id.takeIf { it.isNotBlank() }),
            title = Optional.present(title),
            descriptionHtml = Optional.presentIfNotNull(descriptionHtml),
            handle = Optional.present(handle),
            ruleSet = if (rules.isNotEmpty()) Optional.present(
                CollectionRuleSetInput(
                    appliedDisjunctively = false,
                    rules = Optional.present(ruleInputs),
                )
            ) else Optional.Absent,
        )
    }

    override suspend fun createCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = CollectionCreateMutation(input = collection.toCollectionInput())
            val response = client.mutation(mutation).execute()
            val data = response.data?.collectionCreate
                ?: return SimpleState(isOk = false, msgError = "No response data from collectionCreate")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            val id = data.collection?.id
                ?: return SimpleState(isOk = false, msgError = "collectionCreate returned no id")
            return SimpleState(isOk = true, msgOk = id)
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "createCollection error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun updateCollection(marketplace: Marketplace, collection: ShopifyCollection): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = CollectionUpdateMutation(input = collection.toCollectionInput())
            val response = client.mutation(mutation).execute()
            val data = response.data?.collectionUpdate
                ?: return SimpleState(isOk = false, msgError = "No response data from collectionUpdate")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Updated collection ${collection._id.id}")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "updateCollection error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun deleteCollection(marketplace: Marketplace, collectionId: String): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            val mutation = CollectionDeleteMutation(input = CollectionDeleteInput(id = collectionId))
            val response = client.mutation(mutation).execute()
            val data = response.data?.collectionDelete
                ?: return SimpleState(isOk = false, msgError = "No response data from collectionDelete")
            if (data.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = data.userErrors.joinToString { it.message })
            }
            return SimpleState(isOk = true, msgOk = "Deleted collection ${data.deletedCollectionId}")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "deleteCollection error: ${e.message}")
        } finally {
            client.close()
        }
    }

    override suspend fun uploadImageFromBytes(
        marketplace: Marketplace,
        shopifyId: String,
        imageBytes: ByteArray,
        filename: String,
        altText: String?,
    ): SimpleState {
        val client = buildApolloClient(marketplace)
        try {
            // Step 1: Create staged upload target
            val mimeType = when {
                filename.endsWith(".png", ignoreCase = true) -> "image/png"
                filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            val stagedMutation = StagedUploadsCreateMutation(
                input = listOf(
                    StagedUploadInput(
                        resource = StagedUploadTargetGenerateUploadResource.IMAGE,
                        filename = filename,
                        mimeType = mimeType,
                        fileSize = Optional.present(imageBytes.size.toLong().toString()),
                    )
                )
            )
            val stagedResponse = client.mutation(stagedMutation).execute()
            val stagedData = stagedResponse.data?.stagedUploadsCreate
                ?: return SimpleState(isOk = false, msgError = "No response from stagedUploadsCreate")
            if (stagedData.userErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = stagedData.userErrors.joinToString { it.message })
            }
            val target = stagedData.stagedTargets?.firstOrNull()
                ?: return SimpleState(isOk = false, msgError = "No staged target returned")
            val uploadUrl = target.url.toString()
            val resourceUrl = target.resourceUrl?.toString() ?: return SimpleState(isOk = false, msgError = "No resourceUrl")
            val params = target.parameters.associate { it.name to it.value }

            // Step 2: Upload bytes to staged URL via multipart form
            val boundary = "----FormBoundary${System.currentTimeMillis()}"
            val body = buildMultipartBody(boundary, params, filename, mimeType, imageBytes)
            val httpClient = HttpClient.newHttpClient()
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build()
            val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            if (httpResponse.statusCode() !in 200..299) {
                return SimpleState(isOk = false, msgError = "Staged upload HTTP ${httpResponse.statusCode()}: ${httpResponse.body().take(200)}")
            }

            // Step 3: Attach to product via productCreateMedia
            val mediaMutation = ProductCreateMediaMutation(
                productId = shopifyId,
                media = listOf(
                    CreateMediaInput(
                        originalSource = resourceUrl,
                        alt = Optional.presentIfNotNull(altText),
                        mediaContentType = MediaContentType.IMAGE,
                    )
                )
            )
            val mediaResponse = client.mutation(mediaMutation).execute()
            val mediaData = mediaResponse.data?.productCreateMedia
                ?: return SimpleState(isOk = false, msgError = "No response from productCreateMedia")
            if (mediaData.mediaUserErrors.isNotEmpty()) {
                return SimpleState(isOk = false, msgError = mediaData.mediaUserErrors.joinToString { it.message })
            }
            val mediaId = mediaData.media?.firstOrNull()?.onMediaImage?.id ?: "unknown"
            return SimpleState(isOk = true, msgOk = "Uploaded image $mediaId to $shopifyId")
        } catch (e: Exception) {
            return SimpleState(isOk = false, msgError = "uploadImageFromBytes error: ${e.message}")
        } finally {
            client.close()
        }
    }

    private fun buildMultipartBody(
        boundary: String,
        params: Map<String, String>,
        filename: String,
        mimeType: String,
        fileBytes: ByteArray,
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fun writeLine(s: String) { out.write("$s\r\n".toByteArray()) }
        // Write form fields from staged upload parameters
        for ((key, value) in params) {
            writeLine("--$boundary")
            writeLine("Content-Disposition: form-data; name=\"$key\"")
            writeLine("")
            writeLine(value)
        }
        // Write file part
        writeLine("--$boundary")
        writeLine("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"")
        writeLine("Content-Type: $mimeType")
        writeLine("")
        out.write(fileBytes)
        out.write("\r\n".toByteArray())
        writeLine("--$boundary--")
        return out.toByteArray()
    }
}
