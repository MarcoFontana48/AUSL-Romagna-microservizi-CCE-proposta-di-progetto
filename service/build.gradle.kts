plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.shadowjar)
}

dependencies {

    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.client)
    implementation(libs.vertx.kafka.client)
    implementation(libs.vertx.circuit.breaker)
    implementation(libs.jackson.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.registry.jmx)
    testImplementation(libs.archunit)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    implementation(project(":utils"))
}

application {
    mainClass = "ausl.cce.service.infrastructure.server.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    listOf("distZip", "distTar", "startScripts").forEach {
        named(it) {
            dependsOn(shadowJar)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        // The main class is automatically picked up from the application plugin
    }
}

kotlin {
    jvmToolchain(21)
}