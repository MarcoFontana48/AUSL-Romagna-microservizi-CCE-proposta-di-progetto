plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.registry.jmx)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.bundles.hapi.fhir)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}