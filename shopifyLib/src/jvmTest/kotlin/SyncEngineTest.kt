import com.fonrouge.base.state.SimpleState
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.sync.IErpProductReader
import com.fonrouge.shopifyLib.IShopifyPushService
import com.fonrouge.shopifyLib.engine.ShopifyBidirectionalSyncEngine
import com.fonrouge.shopifyLib.model.ShopifyCollection
import com.fonrouge.shopifyLib.model.SyncReport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncEngineTest {

    private val marketplace = Marketplace(name = "Test Shopify", type = "Shopify")

    private fun product(sku: String, name: String, publishEnabled: Boolean = true) = Product(
        sku = sku,
        name = name,
        baseCost = Money(5000),
        publishEnabled = publishEnabled,
    )

    private fun listing(productId: ProductId, externalId: String, title: String, price: Money = Money(9900)) =
        MarketplaceListing(
            productId = productId,
            marketplaceId = marketplace._id,
            externalId = externalId,
            title = title,
            price = price,
            publishEnabled = true,
        )

    private class FakeErpReader(val products: List<Product>) : IErpProductReader {
        override suspend fun readProducts() = products
        override suspend fun readProductById(erpId: String) = products.find { it.erpSourceId == erpId }
        override suspend fun readInventoryLevels() = products.associate { it.sku to it.inventoryQuantity }
        override suspend fun readPriceLists() = products.associate { it.sku to it.priceLists }
    }

    private class FakePushService : IShopifyPushService {
        val created = mutableListOf<Product>()
        val updated = mutableListOf<Pair<Product, String>>()

        override suspend fun createProduct(marketplace: Marketplace, product: Product) =
            SimpleState(isOk = true, msgOk = "gid://shopify/Product/${created.size + 1}").also { created += product }
        override suspend fun updateProduct(marketplace: Marketplace, product: Product, shopifyId: String) =
            SimpleState(isOk = true, msgOk = "Updated $shopifyId").also { updated += product to shopifyId }
        override suspend fun deleteProduct(marketplace: Marketplace, shopifyId: String) = SimpleState(isOk = true, msgOk = "Deleted")
        override suspend fun updateInventory(marketplace: Marketplace, shopifyId: String, locationId: String, quantity: Int) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun setPublishState(marketplace: Marketplace, shopifyId: String, published: Boolean) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun addToCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun removeFromCollection(marketplace: Marketplace, collectionId: StringId<ShopifyCollection>, shopifyProductId: String) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun uploadImage(marketplace: Marketplace, shopifyId: String, imageUrl: String, altText: String?) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun deleteImage(marketplace: Marketplace, shopifyId: String, imageId: String) = SimpleState(isOk = true, msgOk = "OK")
        override suspend fun reorderImages(marketplace: Marketplace, shopifyId: String, imageIds: List<String>) = SimpleState(isOk = true, msgOk = "OK")
    }

    @Test
    fun `reconcile detects products to create`() = runBlocking {
        val p1 = product("SKU-1", "Product 1")
        val p2 = product("SKU-2", "Product 2")
        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1, p2)),
            pushService = FakePushService(),
            listingsProvider = { emptyList() },
            listingSaver = {},
        )
        val report = engine.reconcile(marketplace)
        assertEquals(2, report.toCreate.size)
        assertEquals(0, report.toUpdate.size)
        assertEquals(0, report.inSync)
    }

    @Test
    fun `reconcile detects in-sync products`() = runBlocking {
        val p1 = product("SKU-1", "Product 1")
        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1)),
            pushService = FakePushService(),
            listingsProvider = { listOf(listing(p1._id, "gid://1", "Product 1", Money(5000))) },
            listingSaver = {},
        )
        val report = engine.reconcile(marketplace)
        assertEquals(0, report.toCreate.size)
        assertEquals(0, report.toUpdate.size)
        assertEquals(1, report.inSync)
    }

    @Test
    fun `reconcile detects title change`() = runBlocking {
        val p1 = product("SKU-1", "New Name")
        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1)),
            pushService = FakePushService(),
            listingsProvider = { listOf(listing(p1._id, "gid://1", "Old Name", Money(5000))) },
            listingSaver = {},
        )
        val report = engine.reconcile(marketplace)
        assertEquals(0, report.toCreate.size)
        assertEquals(1, report.toUpdate.size)
        assertTrue(report.toUpdate[0].changedFields.contains("title"))
    }

    @Test
    fun `reconcile skips unpublished products`() = runBlocking {
        val p1 = product("SKU-1", "Product 1", publishEnabled = false)
        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1)),
            pushService = FakePushService(),
            listingsProvider = { emptyList() },
            listingSaver = {},
        )
        val report = engine.reconcile(marketplace)
        assertEquals(0, report.toCreate.size)
        assertEquals(0, report.toUpdate.size)
        assertEquals(0, report.inSync)
    }

    @Test
    fun `syncToRemote creates products and saves listings`() = runBlocking {
        val p1 = product("SKU-1", "Product 1")
        val pushService = FakePushService()
        val savedListings = mutableListOf<MarketplaceListing>()

        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1)),
            pushService = pushService,
            listingsProvider = { emptyList() },
            listingSaver = { savedListings += it },
        )
        val result = engine.syncToRemote(marketplace)
        assertTrue(!result.hasError)
        assertEquals(1, pushService.created.size)
        assertEquals(1, savedListings.size)
        assertEquals("gid://shopify/Product/1", savedListings[0].externalId)
    }

    @Test
    fun `syncToRemote updates changed products`() = runBlocking {
        val p1 = product("SKU-1", "Updated Name")
        val pushService = FakePushService()

        val engine = ShopifyBidirectionalSyncEngine(
            erpReader = FakeErpReader(listOf(p1)),
            pushService = pushService,
            listingsProvider = { listOf(listing(p1._id, "gid://1", "Old Name", Money(5000))) },
            listingSaver = {},
        )
        val result = engine.syncToRemote(marketplace)
        assertTrue(!result.hasError)
        assertEquals(0, pushService.created.size)
        assertEquals(1, pushService.updated.size)
        assertEquals("gid://1", pushService.updated[0].second)
    }

    @Test
    fun `SyncReport value type creation`() {
        val report = SyncReport(inSync = 5)
        assertEquals(5, report.inSync)
        assertEquals(emptyList(), report.toCreate)
        assertEquals(emptyList(), report.toUpdate)
        assertEquals(emptyList(), report.orphaned)
        assertEquals(emptyList(), report.errors)
    }
}
