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
    testImplementation(kotlin("test"))
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
}

// Configure the application plugin
application {
    mainClass.set("cce.api_gateway.MainKt")
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