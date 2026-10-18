import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.3.0"
    alias(libs.plugins.composeHotReload)
}

val appVersionGeneratedDir = layout.buildDirectory.dir("generated/appVersion/kotlin")

val generateAppVersion by tasks.registering {
    description = "Increments build number and generates AppVersion.kt"
    group = "build"

    val propsFile = layout.projectDirectory.file("version.properties").asFile
    val outputDir = appVersionGeneratedDir

    outputs.dir(outputDir)
    outputs.upToDateWhen { false }

    doLast {
        require(propsFile.exists()) { "Missing ${'$'}{propsFile.path}" }

        val props = mutableMapOf<String, String>()
        propsFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                props[key.trim()] = value.trim()
            }

        val major = props["major"] ?: "0"
        val minor = props["minor"] ?: "0"
        val patch = props["patch"] ?: "0"
        val build = (props["build"]?.toIntOrNull() ?: 0) + 1

        propsFile.writeText(
            """
            |major=$major
            |minor=$minor
            |patch=$patch
            |build=$build
            |""".trimMargin()
        )

        val versionName = "$major.$minor.$patch.$build"
        val packageDir = outputDir.get().asFile.resolve("org/enciyo/agentspawn")
        packageDir.mkdirs()
        packageDir.resolve("AppVersion.kt").writeText(
            """
            |package org.enciyo.agentspawn
            |
            |object AppVersion {
            |    const val NAME = "$versionName"
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(appVersionGeneratedDir)
            dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(libs.arrow.core)
            implementation(libs.androidx.navigation.compose)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-alpha06")

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization.json)
            implementation(libs.ktor.client.js)

            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(generateAppVersion)
}
