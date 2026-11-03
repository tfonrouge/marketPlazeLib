package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.commonLib.model.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Precio de venta vigente de una publicación de Mercado Libre: lo que paga hoy un comprador del canal
 * Mercado Libre, con la promoción que gane en ese momento.
 *
 * Fuente: `GET /items/{id}/sale_price?context=channel_marketplace` (API de Precios, «Precios de
 * productos», documentación oficial actualizada el 2026-10-14). **No es [MercadoLibreProduct.price]**:
 * ese campo viene de `/items` y es el precio publicado **sin** promoción — medido el 2026-11-03 por
 * mppErsaPack: igual al reporte de ML en las 43 publicaciones sin promoción y distinto en las que la
 * tenían (EP-CHA10: 748.50 publicado, 636.22 de venta).
 *
 * El contexto no lleva nivel de lealtad a propósito: una promoción restringida a `buyer_loyalty_N` no es
 * el precio de cualquier comprador. Una promoción **programada** no aparece hasta su fecha de inicio.
 *
 * Los importes son centavos exactos leídos del texto del JSON, nunca de un `Double` (ver [parse]).
 */
@Serializable
data class MercadoLibreSalePrice(
    /** `amount`: el precio que paga el comprador. */
    val amount: Money,
    /**
     * `regular_amount`: el precio sin la promoción; ML sólo lo manda cuando hay una. ML advierte que el
     * precio tachado puede salir de otra fuente y no coincidir con el `regular_amount` de `/prices`.
     */
    val regularAmount: Money? = null,
    val currencyId: String,
    val priceId: String? = null,
    /** `metadata.promotion_id`, p.ej. `OFFER-MLA13456789-1122334455`. */
    val promotionId: String? = null,
    /** `metadata.promotion_type`, p.ej. `custom`. Texto de ML: no hay catálogo cerrado que validar. */
    val promotionType: String? = null,
    /** `reference_date`: el instante para el que ML calculó el precio, tal como lo entrega (ISO-8601). */
    val referenceDate: String? = null,
) {
    /**
     * Hay promoción cuando ML reporta un precio regular **mayor** que el de venta. Un `promotion_id` sin
     * precio regular, o un regular que no supera al de venta, no rebaja nada y no se rotula promoción.
     */
    val hasPromotion: Boolean get() = regularAmount != null && regularAmount > amount

    /**
     * Contrasta este precio con el publicado ([MercadoLibreProduct.price] / `currencyId`). Sin promoción
     * deben ser iguales; si no lo son, algo cambió en ML y hay que verlo — en particular, ML anunció que
     * retirará `price` de `/items`, y ese día el publicado llegará vacío.
     */
    fun compareWithListed(listedPrice: Money, listedCurrencyId: String): SalePriceVsListed = when {
        currencyId != listedCurrencyId -> SalePriceVsListed.CURRENCY_DIFFERENT
        !hasPromotion && amount == listedPrice -> SalePriceVsListed.NO_PROMOTION_SAME
        !hasPromotion -> SalePriceVsListed.NO_PROMOTION_DIFFERENT
        regularAmount == listedPrice -> SalePriceVsListed.PROMOTION_REGULAR_SAME
        else -> SalePriceVsListed.PROMOTION_REGULAR_DIFFERENT
    }

    companion object {
        /** Canal Mercado Libre, sin nivel de lealtad: el precio de cualquier comprador. */
        const val CONTEXT_MARKETPLACE = "channel_marketplace"

        private val json = Json { ignoreUnknownKeys = true }

        /** Igual que [parse] (objeto), desde el cuerpo crudo: lo que no sea un objeto JSON es ilegible. */
        fun parse(body: String): SalePriceParse {
            val element = try {
                json.parseToJsonElement(body)
            } catch (e: Exception) {
                return SalePriceParse.Unreadable("el cuerpo no es JSON: ${e.message?.take(120)}")
            }
            val obj = element as? JsonObject
                ?: return SalePriceParse.Unreadable("el cuerpo no es un objeto JSON")
            return parse(obj)
        }

        /**
         * Parser puro de la respuesta de `/sale_price`.
         *
         * Falla cerrado: `amount` y `currency_id` son obligatorios, y un `regular_amount` **presente** pero
         * ilegible hace ilegible toda la respuesta — descartarlo convertiría una venta en promoción en un
         * precio normal sin que nada lo dijera. Un importe con un tercer decimal significativo se rechaza
         * en vez de redondearse.
         */
        fun parse(obj: JsonObject): SalePriceParse {
            val amountElement = obj["amount"]
            val amount = amountElement.exactCentsOrNull()
                ?: return SalePriceParse.Unreadable("`amount` no es un importe exacto: ${amountElement.describe()}")
            if (amount <= 0) return SalePriceParse.Unreadable("`amount` no es positivo: ${amountElement.describe()}")

            val regularElement = obj["regular_amount"]
            val regular = if (regularElement == null || regularElement is JsonNull) {
                null
            } else {
                regularElement.exactCentsOrNull()?.takeIf { it > 0 }
                    ?: return SalePriceParse.Unreadable(
                        "`regular_amount` no es un importe exacto positivo: ${regularElement.describe()}"
                    )
            }

            val currencyId = obj.text("currency_id")
                ?: return SalePriceParse.Unreadable("sin `currency_id`: el importe no se puede interpretar")
            val metadata = obj["metadata"] as? JsonObject

            return SalePriceParse.Parsed(
                MercadoLibreSalePrice(
                    amount = Money(amount),
                    regularAmount = regular?.let(::Money),
                    currencyId = currencyId,
                    priceId = obj.text("price_id"),
                    promotionId = metadata?.text("promotion_id"),
                    promotionType = metadata?.text("promotion_type"),
                    referenceDate = obj.text("reference_date"),
                )
            )
        }

        private fun JsonObject.text(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.trim()?.ifEmpty { null }

        private fun JsonElement?.describe(): String = when (this) {
            null -> "ausente"
            is JsonNull -> "null"
            else -> toString().take(40)
        }
    }
}

/** Resultado de interpretar una respuesta de `/sale_price`. */
sealed class SalePriceParse {
    data class Parsed(val salePrice: MercadoLibreSalePrice) : SalePriceParse()

    /** La respuesta llegó pero no dice un precio que se pueda usar; [reason] dice por qué. */
    data class Unreadable(val reason: String) : SalePriceParse()
}

/** Cómo se relaciona el precio de venta con el publicado ([MercadoLibreSalePrice.compareWithListed]). */
enum class SalePriceVsListed {
    /** Sin promoción y el precio de venta es el publicado: el caso sano. */
    NO_PROMOTION_SAME,

    /** Sin promoción y distinto del publicado: anomalía que hay que mirar. */
    NO_PROMOTION_DIFFERENT,

    /** Con promoción y su precio regular es el publicado. */
    PROMOTION_REGULAR_SAME,

    /** Con promoción pero el regular no es el publicado (ML avisa que el tachado puede venir de otra fuente). */
    PROMOTION_REGULAR_DIFFERENT,

    /** Monedas distintas: los importes no se comparan. */
    CURRENCY_DIFFERENT,
}

/**
 * Centavos exactos de un número o texto decimal del JSON, sin pasar por coma flotante. `null` si falta, es
 * `null`, no es un decimal simple no negativo (signo, exponente, vacío) o trae un tercer decimal distinto de
 * cero: el peso sólo tiene dos y truncar o redondear perdería dinero en silencio.
 */
internal fun JsonElement?.exactCentsOrNull(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val match = PLAIN_DECIMAL.matchEntire(primitive.content.trim()) ?: return null
    val fraction = match.groupValues[2]
    if (fraction.drop(2).any { it != '0' }) return null
    return match.groupValues[1].toLong() * 100 + fraction.take(2).padEnd(2, '0').toLong()
}

/** Hasta 15 dígitos enteros: el valor en centavos cabe holgado en un `Long`. */
private val PLAIN_DECIMAL = Regex("""^(\d{1,15})(?:\.(\d+))?$""")
