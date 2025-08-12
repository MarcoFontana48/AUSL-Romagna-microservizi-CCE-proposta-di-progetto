plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadowjar)
}

group = "mf.cce"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.client)
    implementation(libs.vertx.kafka.client)
    implementation(libs.vertx.circuit.breaker)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.registry.jmx)
    implementation(libs.jackson.core)
    implementation(libs.jackson.module.kotlin)
    testImplementation(kotlin("test"))
    implementation(project(":utils"))
}

application {
    mainClass.set("cce.api_gateway.infrastructure.server.Main")
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