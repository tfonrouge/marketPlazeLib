plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kvision) apply false
    id("com.apollographql.apollo") version "4.3.3" apply false
}
