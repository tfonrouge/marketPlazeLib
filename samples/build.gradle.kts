plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kilua.rpc)
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
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
            api(project(":shopifyLib"))
            implementation(libs.fonrouge.base)
            implementation(libs.fonrouge.fullStack)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kvision.common.remote)
            implementation(libs.kilua.rpc.ktor)
        }

        jvmMain.dependencies {
            implementation(kotlin("reflect"))
            implementation(libs.fonrouge.mongodb)
            implementation(libs.logback.classic)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
        }
    }
}
