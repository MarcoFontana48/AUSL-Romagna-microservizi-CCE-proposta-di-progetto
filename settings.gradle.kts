dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "CCE_Microservizi"
include(":service")
include(":terapia")
include(":utils")
include(":api_gateway")
include(":end_to_end_tests")