plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    id("maven-publish")
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
            api(libs.fonrouge.base)
            api(libs.fonrouge.fullStack)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.kvision.common.remote)
        }

        jvmMain.dependencies {
            api(kotlin("reflect"))
            api(libs.fonrouge.mongodb)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
        }
    }
}
