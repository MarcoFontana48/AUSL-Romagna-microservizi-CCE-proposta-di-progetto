package architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

/**
 * Test class to verify that the architectural layer dependencies are respected.
 */
internal class DependenciesTest {
    @Test
    fun layerDependenciesAreRespected() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Infrastructure").definedBy("ausl.cce.service.infrastructure..")
            .layer("Application").definedBy("ausl.cce.service.application..")
            .layer("Domain").definedBy("ausl.cce.service.domain..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .check(ClassFileImporter().importPackages("ausl.cce.service..."))
    }
}