package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class MercadoLibreProduct(
    override val _id: StringId<MercadoLibreProduct>,
    val marketplaceId: MarketplaceId,
    val title: String,
    val categoryId: String = "",
    /**
     * `price` de `/items`: el precio **publicado**, sin promoción. Lo que paga el comprador está en
     * [salePrice]. ML anunció que retirará este campo de `/items` en favor de la API de Precios.
     */
    val price: Money = Money.ZERO,
    val currencyId: String = "MXN",
    val availableQuantity: Int = 0,
    val soldQuantity: Int = 0,
    val condition: String = "new",
    val permalink: String = "",
    val thumbnail: String = "",
    val pictures: List<Picture> = emptyList(),
    val status: String = "active",
    val sellerSku: String = "",
) : BaseDoc<StringId<MercadoLibreProduct>> {

    /**
     * Lookup-only: la última observación del precio de venta (con promoción) de esta publicación,
     * proyectada por la aggregation del anfitrión desde `mercadoLibreSalePriceObservation` (mismo `_id`).
     * Default null — nunca observada, o pipeline no configurado. **El sync de productos no la puede pisar**:
     * el dato vive en su propia colección, que sólo escribe el ciclo de [MercadoLibreSalePriceObservation], y
     * aquí sólo se llena al leer. Antes de usar el precio como el de hoy, mirar
     * [MercadoLibreSalePriceObservation.isCurrent].
     */
    var salePrice: MercadoLibreSalePriceObservation? = null

    @Serializable
    data class Picture(
        val id: String,
        val url: String,
        val secureUrl: String? = null,
        val size: String? = null,
    )
}
