package com.fonrouge.shopifyLib.common.annotations

/**
 * Annotation used to specify metadata for a property that is intended to be mapped or interacted with in
 * the context of the Shopify platform. This annotation can be applied to properties to provide additional
 * configuration, such as custom names for serialization/deserialization or marking properties to be ignored.
 *
 * @property name Specifies a custom name to be used for the annotated property. Defaults to an empty string.
 * @property ignore Indicates whether the annotated property should be ignored. If true, the property will not
 *                  be considered during processing. Defaults to false.
 */
@Suppress("unused")
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ShopifyField(
    val name: String = "",
    val ignore: Boolean = false,
)
