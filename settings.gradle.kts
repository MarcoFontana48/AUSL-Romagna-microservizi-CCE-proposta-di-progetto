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
include(":diario_clinico")
include(":anamnesi_pregressa")
include(":utils")
include(":api_gateway")
include(":qa_scenarios")