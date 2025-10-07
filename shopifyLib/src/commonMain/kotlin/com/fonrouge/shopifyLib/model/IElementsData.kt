package com.fonrouge.shopifyLib.model

interface IElementsData<T> {
    val elements: ElementConnection<T>
}
