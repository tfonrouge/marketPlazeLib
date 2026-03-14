package com.fonrouge.commonLib.model

import kotlinx.serialization.Serializable

@Serializable
@kotlin.jvm.JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(factor: Int) = Money(cents * factor.toLong())
    operator fun times(factor: Long) = Money(cents * factor)
    operator fun div(divisor: Int) = Money(cents / divisor.toLong())
    operator fun div(divisor: Long) = Money(cents / divisor)
    operator fun unaryMinus() = Money(-cents)
    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)

        fun fromDecimal(units: Long, decimals: Int = 2): Money {
            var multiplier = 1L
            repeat(decimals) { multiplier *= 10 }
            return Money(units * multiplier)
        }
    }

    fun toDecimal(decimals: Int = 2): Double {
        var divisor = 1.0
        repeat(decimals) { divisor *= 10 }
        return cents / divisor
    }
}
