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
