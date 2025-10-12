package com.fonrouge.shopifyLib.model.graphQL

interface IElementsData<T> {
    val elements: ElementConnection<T>
}