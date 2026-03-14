@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "marketPlazeLib"
include(":commonLib")
include(":shopifyLib")
include(":mercadoLibreLib")
include(":amazonLib")
include(":samples")
