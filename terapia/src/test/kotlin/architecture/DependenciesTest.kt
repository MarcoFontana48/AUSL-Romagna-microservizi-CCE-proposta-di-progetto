package architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

internal class DependenciesTest {
    @Test
    fun layerDependenciesAreRespected() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Infrastructure").definedBy("ausl.cce.terapia.infrastructure..")
            .layer("Application").definedBy("ausl.cce.terapia.application..")
            .layer("Domain").definedBy("ausl.cce.terapia.domain..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .check(ClassFileImporter().importPackages("ausl.cce.terapia..."))
    }
}