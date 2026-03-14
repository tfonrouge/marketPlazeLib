package com.fonrouge.commonLib.colls

import com.fonrouge.commonLib.common.CommonProduct
import com.fonrouge.commonLib.model.Product
import com.fonrouge.commonLib.model.ProductFilter
import com.fonrouge.commonLib.model.ProductId
import com.fonrouge.fullStack.mongoDb.Coll

abstract class ProductColl<UID : Any>(
    debug: Boolean = false,
) : Coll<CommonProduct, Product, ProductId, ProductFilter, UID>(
    commonContainer = CommonProduct,
    debug = debug,
)
