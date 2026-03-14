plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.apollographql.apollo") version "4.3.3"
}

group = "com.fonrouge.marketPlazeLib"
version = libs.versions.marketPlazeLib.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
//    jvmToolchain(21)
    jvm {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
            )
        }
    }
    js(IR) {
        browser {
            useEsModules()
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
                sourceMaps = false
            }
        }
        binaries.library()
        compilerOptions {
            target.set("es2015")
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":commonLib"))
            api("com.apollographql.apollo:apollo-runtime:4.3.3")
            implementation(libs.fonrouge.base)
            implementation(libs.fonrouge.fullStack)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kvision.common.remote)
        }

        jvmMain.dependencies {
            implementation(kotlin("reflect"))
            implementation(libs.fonrouge.mongodb)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
        }
    }
}

apollo {
    service("shopify") {
        packageName.set("com.fonrouge")
        srcDir("src/commonMain/graphql")
        schemaFile.set(file("src/commonMain/graphql/shopify-admin-2025-10.graphqls"))
        generateKotlinModels.set(true)
//        generateFragmentImplementations.set(true)
    }
}
