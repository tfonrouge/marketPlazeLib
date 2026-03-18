import com.fonrouge.commonLib.model.*
import com.fonrouge.commonLib.service.ShippingCalculator
import kotlin.test.Test
import kotlin.test.assertEquals

class ShippingCalculatorTest {

    private val envioTiers = listOf(
        EnvioTier(minWeight = 0.0, maxWeight = 0.5, cost = Money(14000)),
        EnvioTier(minWeight = 0.5, maxWeight = 1.0, cost = Money(14900)),
        EnvioTier(minWeight = 1.0, maxWeight = 2.0, cost = Money(16900)),
        EnvioTier(minWeight = 2.0, maxWeight = 5.0, cost = Money(22000)),
        EnvioTier(minWeight = 5.0, maxWeight = null, cost = Money(24500)),
    )

    private val envioDiscounts = listOf(
        EnvioDiscount(minPrice = 0.0, maxPrice = 299.0, discountRate = 0.30),
        EnvioDiscount(minPrice = 299.0, maxPrice = 499.0, discountRate = 0.60),
        EnvioDiscount(minPrice = 499.0, maxPrice = null, discountRate = 0.50),
    )

    @Test
    fun `volumetric weight calculation`() {
        // 30 x 20 x 15 cm = 9000/5000 = 1.8 kg volumetric
        // real weight = 1.0 kg → billable = 1.8 kg → tier [1, 2) = $169
        val presentation = ProductPresentation(
            label = "Box",
            packageWeight = 1.0,
            packageLength = 30.0,
            packageWidth = 20.0,
            packageHeight = 15.0,
        )
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 0.5,
            presentation = presentation,
            salePrice = Money(100000), // $1000 → no discount match? $499+ → 50%
            envioTiers = envioTiers,
            envioDiscounts = envioDiscounts,
        )
        // billable = max(1.0, 1.8) = 1.8 → tier [1, 2) = 16900
        // discount at $1000 → 50%
        // cost = 16900 * 0.50 = 8450
        assertEquals(Money(8450), cost)
    }

    @Test
    fun `real weight used when no dimensions`() {
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 3.0,
            presentation = null,
            salePrice = Money(40000), // $400 → [299, 499) → 60% discount
            envioTiers = envioTiers,
            envioDiscounts = envioDiscounts,
        )
        // weight 3.0 → tier [2, 5) = 22000
        // discount 60% → 22000 * 0.40 = 8800
        assertEquals(Money(8800), cost)
    }

    @Test
    fun `envio override bypasses calculation`() {
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 10.0,
            presentation = null,
            salePrice = Money(50000),
            envioTiers = envioTiers,
            envioDiscounts = envioDiscounts,
            envioOverride = Money(5000),
        )
        assertEquals(Money(5000), cost)
    }

    @Test
    fun `zero weight returns zero`() {
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 0.0,
            presentation = null,
            salePrice = Money(50000),
            envioTiers = envioTiers,
            envioDiscounts = envioDiscounts,
        )
        assertEquals(Money.ZERO, cost)
    }

    @Test
    fun `empty tiers returns zero`() {
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 5.0,
            presentation = null,
            salePrice = Money(50000),
            envioTiers = emptyList(),
            envioDiscounts = envioDiscounts,
        )
        assertEquals(Money.ZERO, cost)
    }

    @Test
    fun `cargo fijo tier resolution`() {
        val tiers = listOf(
            CargoFijoTier(minPrice = 0.0, maxPrice = 99.0, cost = Money(2500)),
            CargoFijoTier(minPrice = 99.0, maxPrice = 149.0, cost = Money(3000)),
            CargoFijoTier(minPrice = 149.0, maxPrice = 299.0, cost = Money(3700)),
            CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
        )
        assertEquals(Money(2500), ShippingCalculator.resolveCargoFijo(Money(5000), tiers))   // $50
        assertEquals(Money(3000), ShippingCalculator.resolveCargoFijo(Money(10000), tiers))  // $100
        assertEquals(Money(3700), ShippingCalculator.resolveCargoFijo(Money(20000), tiers))  // $200
        assertEquals(Money.ZERO, ShippingCalculator.resolveCargoFijo(Money(50000), tiers))   // $500 → free
    }

    @Test
    fun `cargo fijo override`() {
        val tiers = listOf(
            CargoFijoTier(minPrice = 0.0, maxPrice = 299.0, cost = Money(3700)),
            CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
        )
        assertEquals(Money(1500), ShippingCalculator.resolveCargoFijo(Money(5000), tiers, Money(1500)))
    }

    @Test
    fun `no discount when price below any tier`() {
        val cost = ShippingCalculator.calculateShippingCost(
            productWeight = 0.3,
            presentation = null,
            salePrice = Money(5000), // $50 → [0, 299) → 30% discount
            envioTiers = envioTiers,
            envioDiscounts = envioDiscounts,
        )
        // weight 0.3 → [0, 0.5) = 14000
        // discount 30% → 14000 * 0.70 = 9800
        assertEquals(Money(9800), cost)
    }
}
