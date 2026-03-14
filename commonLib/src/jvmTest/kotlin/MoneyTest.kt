import com.fonrouge.commonLib.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun `addition works correctly`() {
        val a = Money(1000)
        val b = Money(2500)
        assertEquals(Money(3500), a + b)
    }

    @Test
    fun `subtraction works correctly`() {
        val a = Money(5000)
        val b = Money(2000)
        assertEquals(Money(3000), a - b)
    }

    @Test
    fun `multiplication by int works correctly`() {
        val a = Money(1500)
        assertEquals(Money(4500), a * 3)
    }

    @Test
    fun `multiplication by long works correctly`() {
        val a = Money(1500)
        assertEquals(Money(4500), a * 3L)
    }

    @Test
    fun `division works correctly`() {
        val a = Money(9000)
        assertEquals(Money(3000), a / 3)
    }

    @Test
    fun `unary minus works correctly`() {
        val a = Money(1000)
        assertEquals(Money(-1000), -a)
    }

    @Test
    fun `ZERO constant is zero`() {
        assertEquals(0L, Money.ZERO.cents)
    }

    @Test
    fun `comparison works correctly`() {
        assertTrue(Money(1000) < Money(2000))
        assertTrue(Money(3000) > Money(1000))
        assertTrue(Money(1000).compareTo(Money(1000)) == 0)
    }

    @Test
    fun `fromDecimal creates correct amount`() {
        assertEquals(Money(10000), Money.fromDecimal(100, 2))
        assertEquals(Money(100000), Money.fromDecimal(100, 3))
    }

    @Test
    fun `toDecimal returns correct value`() {
        assertEquals(15.50, Money(1550).toDecimal(2), 0.001)
        assertEquals(1.555, Money(1555).toDecimal(3), 0.0001)
    }
}
