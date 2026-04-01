package com.fonrouge.shopifyLib.engine

import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.model.Product
import com.fonrouge.shopifyLib.IShopifyPushService
import com.fonrouge.shopifyLib.model.SmokeTestResult
import kotlinx.coroutines.delay

/**
 * Self-reverting round-trip integration test against a live Shopify store.
 *
 * Flow: fetch product → modify title → push → verify → revert → verify revert.
 * Uses try/finally to guarantee revert even on failure.
 *
 * NOT for automated CI — requires live Shopify credentials.
 */
class ShopifySmokeTest(private val pushService: IShopifyPushService) {

    companion object {
        private const val TEST_SUFFIX = " [SMOKE TEST]"
        private const val FIELD_TESTED = "title"
        private const val API_DELAY_MS = 1000L
    }

    suspend fun run(marketplace: Marketplace, shopifyProductId: String): SmokeTestResult {
        // Step 1: Fetch current product state
        val originalResult = pushService.getProduct(marketplace, shopifyProductId)
        if (originalResult.hasError || originalResult.item == null) {
            return SmokeTestResult(
                productId = shopifyProductId,
                fieldTested = FIELD_TESTED,
                originalValue = null,
                testValue = null,
                updateVerified = false,
                revertVerified = false,
                error = "Failed to fetch product: ${originalResult.msgError}",
            )
        }

        val originalProduct = originalResult.item!!
        val originalTitle = originalProduct.title
        val testTitle = originalTitle + TEST_SUFFIX

        var updateVerified = false
        var revertVerified = false
        var error: String? = null

        try {
            // Step 2: Push modified title
            val updatePayload = Product(
                sku = originalProduct.handle,
                name = testTitle,
            )
            val updateResult = pushService.updateProduct(marketplace, updatePayload, shopifyProductId)
            if (updateResult.hasError) {
                error = "Update failed: ${updateResult.msgError}"
            } else {
                delay(API_DELAY_MS)

                // Step 3: Verify the change landed
                val verifyResult = pushService.getProduct(marketplace, shopifyProductId)
                if (!verifyResult.hasError && verifyResult.item != null) {
                    updateVerified = verifyResult.item!!.title == testTitle
                    if (!updateVerified) {
                        error = "Verification failed: expected '$testTitle', got '${verifyResult.item!!.title}'"
                    }
                } else {
                    error = "Verification fetch failed: ${verifyResult.msgError}"
                }
            }
        } finally {
            // Step 4: ALWAYS revert — even on failure
            delay(API_DELAY_MS)
            val revertPayload = Product(
                sku = originalProduct.handle,
                name = originalTitle,
            )
            val revertResult = pushService.updateProduct(marketplace, revertPayload, shopifyProductId)

            if (!revertResult.hasError) {
                delay(API_DELAY_MS)
                // Step 5: Verify revert
                val revertVerifyResult = pushService.getProduct(marketplace, shopifyProductId)
                if (!revertVerifyResult.hasError && revertVerifyResult.item != null) {
                    revertVerified = revertVerifyResult.item!!.title == originalTitle
                }
            }
            if (!revertVerified && error == null) {
                error = "Revert failed — product may still have test title"
            }
        }

        return SmokeTestResult(
            productId = shopifyProductId,
            fieldTested = FIELD_TESTED,
            originalValue = originalTitle,
            testValue = testTitle,
            updateVerified = updateVerified,
            revertVerified = revertVerified,
            error = error,
        )
    }
}
