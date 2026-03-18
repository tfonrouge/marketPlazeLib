package com.fonrouge.commonLib.service

import com.fonrouge.commonLib.model.*

object MarketplaceDefaults {

    fun mercadoLibreMexico(): Marketplace = Marketplace(
        name = "MercadoLibre MX",
        type = "MercadoLibre",
        ivaRate = 0.08,
        isrRate = 0.025,
        ivaIncluded = true,
        commissionRateClassic = 0.16,
        commissionRatePremium = 0.205,
        isDefault = true,
        minimumPrice = Money(3500), // $35 MXN
        cargoFijoTiers = listOf(
            CargoFijoTier(minPrice = 0.0, maxPrice = 99.0, cost = Money(2500)),
            CargoFijoTier(minPrice = 99.0, maxPrice = 149.0, cost = Money(3000)),
            CargoFijoTier(minPrice = 149.0, maxPrice = 299.0, cost = Money(3700)),
            CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
        ),
        envioTiers = listOf(
            EnvioTier(minWeight = 0.0, maxWeight = 0.3, cost = Money(13100)),
            EnvioTier(minWeight = 0.3, maxWeight = 0.5, cost = Money(14000)),
            EnvioTier(minWeight = 0.5, maxWeight = 1.0, cost = Money(14900)),
            EnvioTier(minWeight = 1.0, maxWeight = 2.0, cost = Money(16900)),
            EnvioTier(minWeight = 2.0, maxWeight = 3.0, cost = Money(19000)),
            EnvioTier(minWeight = 3.0, maxWeight = 4.0, cost = Money(20600)),
            EnvioTier(minWeight = 4.0, maxWeight = 5.0, cost = Money(22000)),
            EnvioTier(minWeight = 5.0, maxWeight = 7.0, cost = Money(24500)),
            EnvioTier(minWeight = 7.0, maxWeight = 9.0, cost = Money(27900)),
            EnvioTier(minWeight = 9.0, maxWeight = 12.0, cost = Money(32300)),
            EnvioTier(minWeight = 12.0, maxWeight = 15.0, cost = Money(38000)),
            EnvioTier(minWeight = 15.0, maxWeight = 20.0, cost = Money(44500)),
            EnvioTier(minWeight = 20.0, maxWeight = 30.0, cost = Money(56300)),
            EnvioTier(minWeight = 30.0, maxWeight = 40.0, cost = Money(69800)),
            EnvioTier(minWeight = 40.0, maxWeight = 50.0, cost = Money(90300)),
            EnvioTier(minWeight = 50.0, maxWeight = 60.0, cost = Money(101400)),
            EnvioTier(minWeight = 60.0, maxWeight = 70.0, cost = Money(104100)),
            EnvioTier(minWeight = 70.0, maxWeight = 80.0, cost = Money(108400)),
            EnvioTier(minWeight = 80.0, maxWeight = 90.0, cost = Money(121900)),
            EnvioTier(minWeight = 90.0, maxWeight = 100.0, cost = Money(140600)),
            EnvioTier(minWeight = 100.0, maxWeight = null, cost = Money(159300)),
        ),
        envioDiscounts = listOf(
            EnvioDiscount(minPrice = 0.0, maxPrice = 299.0, discountRate = 0.30),
            EnvioDiscount(minPrice = 299.0, maxPrice = 499.0, discountRate = 0.60),
            EnvioDiscount(minPrice = 499.0, maxPrice = null, discountRate = 0.50),
        ),
    )

    fun amazonMexico(): Marketplace = Marketplace(
        name = "Amazon MX",
        type = "Amazon",
        ivaRate = 0.08,
        isrRate = 0.025,
        ivaIncluded = true,
        commissionRateClassic = 0.15,
        commissionRatePremium = 0.15,
        isDefault = true,
        cargoFijoTiers = listOf(
            CargoFijoTier(minPrice = 0.0, maxPrice = 99.0, cost = Money(2500)),
            CargoFijoTier(minPrice = 99.0, maxPrice = 149.0, cost = Money(3000)),
            CargoFijoTier(minPrice = 149.0, maxPrice = 299.0, cost = Money(3700)),
            CargoFijoTier(minPrice = 299.0, maxPrice = null, cost = Money.ZERO),
        ),
    )
}
