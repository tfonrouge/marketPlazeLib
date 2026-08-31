package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MercadoLibreOrder(
    override val _id: StringId<MercadoLibreOrder>,
    val marketplaceId: MarketplaceId,
    val dateCreated: Instant,
    val lastUpdated: Instant,
    val status: String = "",
    val totalAmount: Money = Money.ZERO,
    val paidAmount: Money = Money.ZERO,
    val currencyId: String = "MXN",
    val buyer: Buyer = Buyer(),
    val orderItems: List<OrderItem> = emptyList(),
    val shipping: ShippingInfo = ShippingInfo(),
    val payments: List<Payment> = emptyList(),
    val packId: Long? = null,
) : BaseDoc<StringId<MercadoLibreOrder>> {

    /**
     * Lookup-only field populated by host-app aggregation pipeline (e.g., via
     * `lookupField(coll = MlOrderReviewColl, ...)` on the order collection registration).
     * Default null — no review yet, or pipeline not configured. Never persisted by sync.
     */
    var mlOrderReview: MlOrderReview? = null

    /**
     * Lookup-only: snapshot logístico del envío (FULL/Normal) proyectado por la aggregation del
     * host-app desde la colección de documentos de la orden (host-side `lookupAnyField(...,
     * pipeline = [replaceWith "$shipment"])`). Tipo NEUTRO (sin dependencia downstream del host).
     * Default null — sin envío capturado, o pipeline no configurado. Nunca persistido por sync.
     */
    var mlShipment: MlShipmentInfo? = null

    /**
     * Lookup-only: estado fiscal de la venta proyectado por la aggregation del host desde su propia
     * colección de documentos. Tipo NEUTRO (sin dependencia downstream del host). Default null —
     * sin observación fiscal, o pipeline no configurado. Nunca persistido por sync.
     *
     * **No lleva datos fiscales del comprador**: la proyección excluye deliberadamente RFC y
     * domicilio del receptor. Lo que viaja acá es estado e identidad del comprobante, que puede
     * pintarse en una lista sin control de acceso por documento; los datos personales exigen una
     * ruta propia con autorización, no un campo hidratado en cada renglón.
     */
    var mlFiscal: MlFiscalInfo? = null

    /**
     * Subconjunto neutro del estado fiscal de la venta. Todos los campos son opcionales: una orden
     * puede tener estado sin comprobante (aún no emitido, o no corresponde).
     *
     * [estado] viaja como `String` a propósito — el catálogo de estados lo define el host, y esta
     * librería no puede depender de él sin invertir la dirección de las dependencias.
     */
    @Serializable
    data class MlFiscalInfo(
        val estado: String? = null,
        val uuid: String? = null,
        val serie: String? = null,
        val folio: String? = null,
        val fecha: String? = null,
        val totalCents: Long? = null,
        val verificadoAt: Instant? = null,
    )

    /** Subconjunto neutro del snapshot logístico de la orden (FULL/Normal). `fulfillment` = FULL. */
    @Serializable
    data class MlShipmentInfo(
        val logisticType: String? = null,
        val status: String? = null,
        val substatus: String? = null,
    )

    @Serializable
    data class Buyer(
        val id: Long = 0,
        val nickname: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phone: String = "",
    )

    @Serializable
    data class OrderItem(
        /**
         * El id del listing de ML (`item.id`, p.ej. `MLM5072161188`) — **la identidad real de la linea**.
         *
         * El [sku] (`seller_sku`) **no sirve como identidad**: es una **etiqueta editable** por el
         * vendedor, puede venir **vacia** (hay listings activos sin `seller_sku`) y dos publicaciones
         * distintas pueden compartirla. [itemId] es inmutable y lo asigna ML.
         *
         * Lo consume el pipeline de pedidos de ErsaPack (LEDGER D79) para casar cada linea de la orden
         * con su partida: sin una identidad estable no se puede verificar que un pedido este completo,
         * y un pedido parcial consolidado en silencio es un descuadre fiscal.
         *
         * Default `""` para no romper los documentos ya persistidos: se rellenan en el proximo sync.
         */
        val itemId: String = "",
        val title: String = "",
        val quantity: Int = 0,
        val unitPrice: Money = Money.ZERO,
        val sku: String = "",
        val variationId: String? = null,
    )

    @Serializable
    data class ShippingInfo(
        val id: Long? = null,
        val status: String = "",
        val trackingNumber: String? = null,
        val receiverAddress: ReceiverAddress = ReceiverAddress(),
    )

    @Serializable
    data class ReceiverAddress(
        val zipCode: String = "",
        val city: String = "",
        val state: String = "",
    )

    @Serializable
    data class Payment(
        val id: Long = 0,
        val status: String = "",
        val transactionAmount: Money = Money.ZERO,
        val paymentMethodId: String = "",
    )
}
