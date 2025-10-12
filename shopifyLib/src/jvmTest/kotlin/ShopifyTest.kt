import com.fonrouge.GetProductsQuery
import com.fonrouge.shopifyLib.common.annotations.ShopifyField
import com.fonrouge.shopifyLib.model.product.ShopifyProduct
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopifyTest {
    private fun mapOfAnnotatedClass(kClass: KClass<*>): List<String> {
        return kClass.memberProperties.mapNotNull { property ->
            val shopifyField = property.findAnnotation<ShopifyField>()
            shopifyField?.name.isNullOrEmpty()
            property.findAnnotation<ShopifyField>()?.let { shopifyField ->
                if (shopifyField.ignore) return@mapNotNull null
                if (shopifyField.name.isEmpty()) return@mapNotNull null
                shopifyField.name
            } ?: property.name
        }.sorted()
    }

    @Test
    fun `shopify product properties test`() {
        assertEquals(
            expected = mapOfAnnotatedClass(ShopifyProduct::class),
            actual = GetProductsQuery.Node::class.memberProperties.map { it.name }.sorted()
        )
    }
}
