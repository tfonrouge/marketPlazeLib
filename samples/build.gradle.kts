import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kilua.rpc)
    alias(libs.plugins.kvision)
}

version = libs.versions.marketPlazeLib.get()
group = "com.fonrouge.samples"

repositories {
    mavenCentral()
    mavenLocal()
}

val mainClassName = "io.ktor.server.netty.EngineMain"

val copyJsBundleToAssets by tasks.registering(Copy::class) {
    group = "application"
    description = "Copies the webpack development bundle and index.html into the JVM classpath under /assets"
    dependsOn("jsBrowserDevelopmentWebpack", "jsProcessResources")
    from(tasks.named("jsBrowserDevelopmentWebpack").map { it.outputs.files })
    from(layout.buildDirectory.dir("processedResources/js/main"))
    into(layout.buildDirectory.dir("processedResources/jvm/main/assets"))
}

tasks.matching { it.name == "jvmRun" }.configureEach {
    dependsOn(copyJsBundleToAssets)
}

tasks.register("run") {
    group = "application"
    description = "Builds the JS frontend and starts the Ktor backend on http://localhost:8080"
    dependsOn(copyJsBundleToAssets)
    finalizedBy("jvmRun")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set(mainClassName)
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
        binaries.executable()
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
            implementation(libs.fonrouge.memorydb)
            implementation(libs.logback.classic)
            implementation("io.ktor:ktor-server-netty:3.4.0")
            implementation("io.ktor:ktor-server-compression:3.4.0")
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
        }
    }
}
