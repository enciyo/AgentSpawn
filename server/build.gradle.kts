import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "org.enciyo.agentspawn"
version = "1.0.0"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val env = System.getenv("KTOR_ENV") ?: "development"
    val isDevelopment = env == "development"
    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=$isDevelopment",
        "-Dlogback.configurationFile=logback-app.xml"
    )
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.cors)
    implementation(libs.ktor.swagger)
    implementation(libs.swagger.codegen.generator)
    implementation(libs.ktor.openapi)
    implementation(libs.ktor.routingOpenapi)
    implementation(libs.swagger.codegen.generator)
    implementation(libs.arrow.core)

    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)

    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.di)
    implementation(libs.ktor.client.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.redoc)
    implementation(libs.ktor.requestValidation)
    implementation(libs.ktor.auth)
    implementation(libs.ktor.authApiKey)
    implementation(libs.ktor.hostCommon)
    implementation(libs.ktor.statusPages)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

ktor {
    openApi {
        enabled = true
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin
    .compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters", "-Xsam-conversions=class"))
    }

