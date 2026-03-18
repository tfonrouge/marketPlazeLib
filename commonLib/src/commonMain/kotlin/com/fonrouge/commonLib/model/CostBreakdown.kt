package com.fonrouge.commonLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.OId
import kotlinx.serialization.Serializable

typealias CostBreakdownId = OId<CostBreakdown>

@Serializable
data class CostBreakdown(
    override val _id: CostBreakdownId = OId(),
    val productId: ProductId,
    val marketplaceId: MarketplaceId,
    val listingId: MarketplaceListingId? = null,
    val salePrice: Money,
    val finalPrice: Money,
    val baseCost: Money,
    val taxableBase: Money = Money.ZERO,
    val ivaWithheld: Money = Money.ZERO,
    val isrWithheld: Money = Money.ZERO,
    val shippingCost: Money = Money.ZERO,
    val cargoFijo: Money = Money.ZERO,
    val costoEnvioLogistico: Money = Money.ZERO,
    val envioCliente: Money = Money.ZERO,
    val referralKitCost: Money = Money.ZERO,
    val lineItems: List<FeeLineItem> = emptyList(),
    val totalCosts: Money,
    val netProfit: Money,
    val profitMarginPercent: Double,
    val healthLevel: String = HealthLevel.CRITICAL.name,
    val piecesCount: Int? = null,
    val pricePerPiece: Money? = null,
    val costPerPiece: Money? = null,
    val profitPerPiece: Money? = null,
) : BaseDoc<CostBreakdownId>
