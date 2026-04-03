plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.apollo)
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
            api(project(":commonLib"))
            api(libs.apollo.runtime)
        }

        jvmMain.dependencies {
            implementation(kotlin("reflect"))
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
