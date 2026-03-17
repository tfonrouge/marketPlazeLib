import com.fonrouge.commonLib.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductEnrichmentTest {

    private val json = Json { prettyPrint = false; encodeDefaults = true }

    @Test
    fun `enriched product serialization round-trip`() {
        val product = Product(
            sku = "TEST-001",
            name = "Test Product",
            baseCost = Money(5000),
            erpSourceId = "ERP-42",
            priceLists = listOf(
                PriceListEntry(name = "Retail", price = Money(9900)),
                PriceListEntry(name = "Wholesale", price = Money(7500), compareAtPrice = Money(9900)),
            ),
            inventoryQuantity = 100,
            descriptionHtml = "<p>Test description</p>",
            publishEnabled = true,
            status = "active",
            tags = listOf("test", "electronics"),
            weight = 1.5,
            weightUnit = "kg",
            barcode = "1234567890123",
            seoTitle = "Test Product | My Store",
            seoDescription = "Best test product available",
        )

        val encoded = json.encodeToString(product)
        val decoded = json.decodeFromString<Product>(encoded)

        assertEquals(product, decoded)
        assertEquals("ERP-42", decoded.erpSourceId)
        assertEquals(2, decoded.priceLists.size)
        assertEquals(Money(7500), decoded.priceLists[1].price)
        assertEquals(Money(9900), decoded.priceLists[1].compareAtPrice)
        assertEquals(100, decoded.inventoryQuantity)
        assertEquals(true, decoded.publishEnabled)
        assertEquals("active", decoded.status)
        assertEquals(listOf("test", "electronics"), decoded.tags)
        assertEquals("kg", decoded.weightUnit)
        assertEquals("1234567890123", decoded.barcode)
    }

    @Test
    fun `enriched product defaults are backward compatible`() {
        val minimal = Product(sku = "MIN-001", name = "Minimal")
        assertEquals(null, minimal.erpSourceId)
        assertEquals(emptyList(), minimal.priceLists)
        assertEquals(0, minimal.inventoryQuantity)
        assertEquals(null, minimal.descriptionHtml)
        assertEquals(false, minimal.publishEnabled)
        assertEquals("draft", minimal.status)
        assertEquals(emptyList(), minimal.tags)
        assertEquals(null, minimal.weight)
        assertEquals(null, minimal.barcode)
        assertEquals(null, minimal.seoTitle)
    }

    @Test
    fun `enriched variant serialization`() {
        val variant = ProductVariant(
            sku = "VAR-001",
            name = "Red Large",
            price = Money(9900),
            inventory = 50,
            compareAtPrice = Money(12900),
            barcode = "VAR-BC-001",
            weight = 0.5,
            weightUnit = "kg",
            options = mapOf("Color" to "Red", "Size" to "Large"),
        )

        val encoded = json.encodeToString(variant)
        val decoded = json.decodeFromString<ProductVariant>(encoded)

        assertEquals(variant, decoded)
        assertEquals(Money(12900), decoded.compareAtPrice)
        assertEquals("Red", decoded.options["Color"])
        assertEquals("Large", decoded.options["Size"])
    }

    @Test
    fun `enriched image with position`() {
        val image = ProductImage(url = "https://example.com/img.jpg", position = 2)
        val encoded = json.encodeToString(image)
        val decoded = json.decodeFromString<ProductImage>(encoded)
        assertEquals(2, decoded.position)
    }

    @Test
    fun `product option serialization`() {
        val option = ProductOption(name = "Size", values = listOf("S", "M", "L", "XL"))
        val encoded = json.encodeToString(option)
        val decoded = json.decodeFromString<ProductOption>(encoded)
        assertEquals("Size", decoded.name)
        assertEquals(4, decoded.values.size)
    }

    @Test
    fun `product filter with new fields`() {
        val filter = ProductFilter(publishEnabled = true, status = "active")
        assertEquals(true, filter.publishEnabled)
        assertEquals("active", filter.status)
        // Copy pattern
        val updated = filter.copy(status = "draft")
        assertEquals("draft", updated.status)
        assertEquals(true, updated.publishEnabled)
    }
}
