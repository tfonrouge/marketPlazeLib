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
    jvmToolchain(25)
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
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }

        jsMain.dependencies {
        }
    }
}
