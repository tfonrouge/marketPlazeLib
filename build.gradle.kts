plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kvision) apply false
    alias(libs.plugins.apollo) apply false
    alias(libs.plugins.dokka) apply false
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().resolution("node-fetch", "2.7.0")
}
