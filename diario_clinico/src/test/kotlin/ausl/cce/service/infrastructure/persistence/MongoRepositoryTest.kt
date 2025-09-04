package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterFactory
import ausl.cce.service.domain.EncounterId
import ausl.cce.service.domain.toJson
import mf.cce.utils.DockerTest
import mf.cce.utils.RepositoryCredentials
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.io.File

class MongoEncounterRepositoryTest : DockerTest() {
    private val dummyTestEntity = DummyEntity.of(DummyEntity.DummyId("test-123"), "dummy-field")

    val encounterTest = EncounterFactory.of(
        id = "123",
        patientReference = "Patient/123",
        encounterClass = "AMB",
        status = "finished",
        serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
        serviceTypeCode = "408",
        serviceTypeDisplay = "General Medicine"
    )
    private val encounterTestWrapperEntity = EncounterEntity.of(encounterTest)

    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/persistence/mongoDbDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var dummyRepository: DummyRepository
    private lateinit var encounterRepository: EncounterRepository

    val repositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "diario-clinico-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "diario-clinico-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeUp(dockerComposeFile)

        // connection is done here, if it fails, all tests will fail, so it can be tested here like this
        dummyRepository = MongoDummyRepository(repositoryCredentials)
        encounterRepository = MongoEncounterRepository(repositoryCredentials)
    }

    @AfterEach
    fun tearDown() {
        try {
            dummyRepository.close()
            encounterRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing repository: ${e.message}")
        }
        executeDockerComposeDown(dockerComposeFile)
    }

    @Test
    @DisplayName("Test saving and finding an existing entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun saveAndFindById() {
        logger.trace("Saving entity with id: {}", dummyTestEntity)
        val expectedEntity = dummyTestEntity
        dummyRepository.save(dummyTestEntity)

        val foundEntity = dummyRepository.findById(DummyEntity.DummyId("test-123"))

        assertAll(
            { assertNotNull(foundEntity) {
                "Entity with id 'test-123' not found after saving. " +
                        "This indicates a failure in the save operation or the findById method."
            }},
            { assertEquals(expectedEntity, foundEntity) {
                "Expected entity $expectedEntity, but found $foundEntity. " +
                        "This indicates a failure in the findById method."
            }}
        )
    }

    @Test
    @DisplayName("Test finding a non-existent entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun findByIdNonExistent() {
        val nonExistentEntity = DummyEntity.of(DummyEntity.DummyId("non-existent"), "dummy-field")

        val foundEntity = dummyRepository.findById(nonExistentEntity.id)

        assertAll(
            { assertNull(foundEntity) {
                "Expected no entity to be found for ID '${nonExistentEntity.id.value}', " +
                        "but found: ${foundEntity?.id?.value}. " +
                        "This indicates a failure in the findById method, as it should return null for non-existent IDs."
            }}
        )
    }

    @Test
    @DisplayName("Test finding all entities")
    @Timeout(5 * 60) // 5 minutes timeout
    fun findAll() {
        logger.trace("Testing findAll functionality")

        // Save multiple test entities
        val testEntity1 = DummyEntity.of(DummyEntity.DummyId("test-123"), "dummy-field-1")
        val testEntity2 = DummyEntity.of(DummyEntity.DummyId("test-456"), "dummy-field-2")
        val testEntity3 = DummyEntity.of(DummyEntity.DummyId("test-789"), "dummy-field-3")

        dummyRepository.save(testEntity1)
        dummyRepository.save(testEntity2)
        dummyRepository.save(testEntity3)

        logger.trace("Saved additional entities: test-456, test-789")

        val allEntities = dummyRepository.findAll().toList()
        logger.trace("Found ${allEntities.size} entities")

        allEntities.forEach { entity ->
            logger.trace("Entity ID: ${entity.id.value}")
        }

        val entityIds = allEntities.map { it.id.value }.toSet()

        assertAll(
            { assertTrue(allEntities.size >= 3) {
                "FindAll test failed - expected at least 3 entities, found ${allEntities.size}. " +
                        "This indicates a failure in the findAll or save methods."
            }},
            { assertTrue(entityIds.contains("test-123")) { "Entity test-123 not found in findAll results" }},
            { assertTrue(entityIds.contains("test-456")) { "Entity test-456 not found in findAll results" }},
            { assertTrue(entityIds.contains("test-789")) { "Entity test-789 not found in findAll results" }}
        )
    }

    @Test
    @DisplayName("Test updating an existing entity")
    @Timeout(5 * 60) // 5 minutes timeout
    fun updateExistingEntity() {
        logger.trace("Testing update functionality for existing entity")

        // First save an entity
        val originalEntity = DummyEntity.of(DummyEntity.DummyId("test-123"), "original-field")
        dummyRepository.save(originalEntity)

        // Create updated entity with same ID
        val entityToUpdate = DummyEntity.of(DummyEntity.DummyId("test-123"), "updated-field")
        logger.trace("Updating entity with id: ${entityToUpdate.id.value}")

        // Update should not throw exception for existing entity
        dummyRepository.update(entityToUpdate)

        // Verify the entity still exists after update
        val updatedEntity = dummyRepository.findById(DummyEntity.DummyId("test-123"))

        assertAll(
            { assertNotNull(updatedEntity) {
                "Entity not found after update. This indicates a failure in the update method."
            }},
            { assertEquals(entityToUpdate, updatedEntity) {
                "Updated entity does not match expected entity"
            }}
        )
    }

    @Test
    @DisplayName("Test updating a non-existent entity should throw exception")
    @Timeout(5 * 60) // 5 minutes timeout
    fun updateNonExistentEntity() {
        logger.trace("Testing update functionality for non-existent entity")

        val nonExistentEntity = DummyEntity.of(DummyEntity.DummyId("non-existent-update"), "dummy-field")

        // Update should throw RuntimeException for non-existent entity
        val exception = assertThrows<RuntimeException> {
            dummyRepository.update(nonExistentEntity)
        }

        logger.trace("Correctly threw exception for non-existent entity update: ${exception.message}")

        assertAll(
            { assertNotNull(exception) {
                "Expected RuntimeException to be thrown for non-existent entity update"
            }},
            { assertNotNull(exception.message) {
                "Exception should have a meaningful error message"
            }}
        )
    }

    @Test
    @DisplayName("Test deleting an existing entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun deleteExistingEntityById() {
        logger.trace("Testing deleteById functionality for existing entity")

        // First save entities to delete
        val entityToDelete = DummyEntity.of(DummyEntity.DummyId("test-456"), "dummy-field-to-delete")
        dummyRepository.save(entityToDelete)

        // Delete the entity
        val deletedEntity = dummyRepository.deleteById(DummyEntity.DummyId("test-456"))

        // Verify the entity no longer exists
        val shouldBeNull = dummyRepository.findById(DummyEntity.DummyId("test-456"))

        assertAll(
            { assertNotNull(deletedEntity) {
                "Delete operation should return the deleted entity, but returned null"
            }},
            { assertEquals(entityToDelete, deletedEntity) {
                "Deleted entity does not match the expected entity"
            }},
            { assertNull(shouldBeNull) {
                "Entity still exists after deletion. This indicates a failure in the deleteById method."
            }}
        )
    }

    @Test
    @DisplayName("Test deleting a non-existent entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun deleteNonExistentEntityById() {
        logger.trace("Testing deleteById functionality for non-existent entity")

        val nonExistentDelete = dummyRepository.deleteById(DummyEntity.DummyId("definitely-not-exists"))

        assertAll(
            { assertNull(nonExistentDelete) {
                "Delete operation should return null for non-existent entity, " +
                        "but returned: $nonExistentDelete"
            }}
        )
    }

    @Test
    @DisplayName("Complete CRUD operations workflow test")
    @Timeout(5 * 60) // 5 minutes timeout
    fun completeCrudWorkflow() {
        logger.trace("Testing complete CRUD workflow")

        // Create and save multiple entities
        val entity1 = DummyEntity.of(DummyEntity.DummyId("workflow-1"), "dummy-field-1")
        val entity2 = DummyEntity.of(DummyEntity.DummyId("workflow-2"), "dummy-field-2")
        val entity3 = DummyEntity.of(DummyEntity.DummyId("workflow-3"), "dummy-field-3")

        dummyRepository.save(entity1)
        dummyRepository.save(entity2)
        dummyRepository.save(entity3)

        // Verify all entities exist
        val allEntities = dummyRepository.findAll().toList()

        // Update one entity
        val updatedEntity2 = DummyEntity.of(DummyEntity.DummyId("workflow-2"), "updated-field-2")
        dummyRepository.update(updatedEntity2)

        // Delete one entity
        val deletedEntity = dummyRepository.deleteById(DummyEntity.DummyId("workflow-1"))

        // Final state check
        val finalEntities = dummyRepository.findAll().toList()
        logger.trace("Final entity count: ${finalEntities.size}")

        finalEntities.forEach { entity ->
            logger.trace("Remaining entity: ${entity.id.value}")
        }

        val finalEntityIds = finalEntities.map { it.id.value }.toSet()

        assertAll(
            { assertTrue(allEntities.size >= 3) {
                "Expected at least 3 entities after saving, found ${allEntities.size}"
            }},
            { assertNotNull(deletedEntity) { "Failed to delete entity workflow-1" }},
            { assertFalse(finalEntityIds.contains("workflow-1")) { "Entity workflow-1 should have been deleted" }},
            { assertTrue(finalEntityIds.contains("workflow-2")) { "Entity workflow-2 should still exist" }},
            { assertTrue(finalEntityIds.contains("workflow-3")) { "Entity workflow-3 should still exist" }}
        )
    }

    // === ENCOUNTER TESTS ===

    @Test
    @DisplayName("Test saving and finding an existing encounter entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun saveAndFindByEncounterId() {
        logger.trace("Saving encounter entity with id: {}", encounterTestWrapperEntity)
        val expectedEntity = encounterTestWrapperEntity
        encounterRepository.save(encounterTestWrapperEntity)

        val foundEntity = encounterRepository.findById(EncounterId("123"))

        assertAll(
            { assertNotNull(foundEntity) {
                "Encounter entity with id '123' not found after saving. " +
                        "This indicates a failure in the save operation or the findById method."
            }},
            { assertEquals(expectedEntity, foundEntity) {
                "Expected encounter entity $expectedEntity, but found $foundEntity. " +
                        "This indicates a failure in the findById method."
            }},
            { assertEquals(encounterTest.toJson(), foundEntity?.encounter?.toJson()) {
                "Expected FHIR encounter resource ${encounterTest.toJson()}, but found ${foundEntity?.encounter?.toJson()}. " +
                        "This indicates a failure in the findById method."
            }}
        )
    }

    @Test
    @DisplayName("Test finding a non-existent encounter entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun findByEncounterIdNonExistent() {
        val nonExistentEntity = EncounterEntity.of(
            EncounterFactory.of(
                id = "999",
                patientReference = "Patient/999",
                encounterClass = "NONEXISTENT",
                status = "cancelled"
            )
        )
        val foundEntity = encounterRepository.findById(nonExistentEntity.id)
        assertAll(
            {
                assertNull(foundEntity) {
                    "Expected no encounter entity to be found for ID '${nonExistentEntity.id.value}', " +
                            "but found: ${foundEntity?.id?.value}. " +
                            "This indicates a failure in the findById method, as it should return null for non-existent IDs."
                }
            }
        )
    }

    @Test
    @DisplayName("Test finding all encounter entities")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun findAllEncounterEntities() {
        logger.trace("Testing findAll functionality for encounter entities")

        // Save multiple test entities
        val testEntity1 = EncounterEntity.of(
            EncounterFactory.of(
                id = "123",
                patientReference = "Patient/123",
                encounterClass = "AMB",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "408",
                serviceTypeDisplay = "General Medicine"
            )
        )
        val testEntity2 = EncounterEntity.of(
            EncounterFactory.of(
                id = "456",
                patientReference = "Patient/456",
                encounterClass = "IMP",
                status = "in-progress",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "394802001",
                serviceTypeDisplay = "Cardiology"
            )
        )
        val testEntity3 = EncounterEntity.of(
            EncounterFactory.of(
                id = "789",
                patientReference = "Patient/789",
                encounterClass = "EMER",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "773568002",
                serviceTypeDisplay = "Emergency Medicine"
            )
        )

        encounterRepository.save(testEntity1)
        encounterRepository.save(testEntity2)
        encounterRepository.save(testEntity3)

        logger.trace("Saved additional encounter entities: encounter-456, encounter-789")

        val allEntities = encounterRepository.findAll().toList()
        logger.trace("Found ${allEntities.size} encounter entities")

        allEntities.forEach { entity ->
            logger.trace("Encounter Entity ID: ${entity.id.value}")
        }

        val entityIds = allEntities.map { it.id.value }.toSet()

        assertAll(
            {
                assertTrue(allEntities.size >= 3) {
                    "FindAll test failed - expected at least 3 encounter entities, found ${allEntities.size}. " +
                            "This indicates a failure in the findAll or save methods."
                }
            },
            { assertTrue(entityIds.contains("123")) { "Encounter entity 123 not found in findAll results" } },
            { assertTrue(entityIds.contains("456")) { "Encounter entity 456 not found in findAll results" } },
            { assertTrue(entityIds.contains("789")) { "Encounter entity 789 not found in findAll results" } }
        )
    }

    @Test
    @DisplayName("Test updating an existing encounter entity")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun updateExistingEncounterEntity() {
        logger.trace("Testing update functionality for existing encounter entity")

        // First save an entity
        val originalEntity = EncounterEntity.of(
            EncounterFactory.of(
                id = "123",
                patientReference = "Patient/123",
                encounterClass = "AMB",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "408",
                serviceTypeDisplay = "General Medicine"
            )
        )
        encounterRepository.save(originalEntity)

        // Create updated entity with same ID
        val updatedEntity = EncounterEntity.of(
            EncounterFactory.of(
                id = "123",
                patientReference = "Patient/123",
                encounterClass = "IMP",
                status = "in-progress",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "394802001",
                serviceTypeDisplay = "Cardiology"
            )
        )
        logger.trace("Updating encounter entity with id: ${updatedEntity.id.value}")

        // Update should not throw exception for existing entity
        encounterRepository.update(updatedEntity)

        // Verify the entity still exists after update
        val updatedEntityFromDb = encounterRepository.findById(EncounterId("123"))

        assertAll(
            {
                assertNotNull(updatedEntityFromDb) {
                    "Encounter entity not found after update. This indicates a failure in the update method."
                }
            },
            {
                assertEquals(updatedEntity, updatedEntityFromDb) {
                    "Updated encounter entity does not match expected entity"
                }
            }
        )
    }

    @Test
    @DisplayName("Test updating a non-existent encounter entity should throw exception")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun updateNonExistentEncounterEntity() {
        logger.trace("Testing update functionality for non-existent encounter entity")

        val nonExistentEntity = EncounterEntity.of(
            EncounterFactory.of(
                id = "non-existent-update",
                patientReference = "Patient/999",
                encounterClass = "AMB",
                status = "cancelled"
            )
        )

        // Update should throw RuntimeException for non-existent entity
        val exception = assertThrows<RuntimeException> {
            encounterRepository.update(nonExistentEntity)
        }

        logger.trace("Correctly threw exception for non-existent encounter entity update: ${exception.message}")

        assertAll(
            {
                assertNotNull(exception) {
                    "Expected RuntimeException to be thrown for non-existent encounter entity update"
                }
            },
            {
                assertNotNull(exception.message) {
                    "Exception should have a meaningful error message"
                }
            }
        )
    }

    @Test
    @DisplayName("Test deleting an existing encounter entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun deleteExistingEncounterEntityById() {
        logger.trace("Testing deleteById functionality for existing encounter entity")

        // First save entity to delete
        val entityToDelete = EncounterEntity.of(
            EncounterFactory.of(
                id = "456",
                patientReference = "Patient/456",
                encounterClass = "EMER",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "773568002",
                serviceTypeDisplay = "Emergency Medicine"
            )
        )
        encounterRepository.save(entityToDelete)

        // Delete the entity
        val deletedEntity = encounterRepository.deleteById(EncounterId("456"))

        // Verify the entity no longer exists
        val shouldBeNull = encounterRepository.findById(EncounterId("456"))

        assertAll(
            {
                assertNotNull(deletedEntity) {
                    "Delete operation should return the deleted encounter entity, but returned null"
                }
            },
            {
                assertEquals(entityToDelete, deletedEntity) {
                    "Deleted encounter entity does not match the expected entity"
                }
            },
            {
                assertNull(shouldBeNull) {
                    "Encounter entity still exists after deletion. This indicates a failure in the deleteById method."
                }
            }
        )
    }

    @Test
    @DisplayName("Test deleting a non-existent encounter entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun deleteNonExistentEncounterEntityById() {
        logger.trace("Testing deleteById functionality for non-existent encounter entity")

        val nonExistentDelete = encounterRepository.deleteById(EncounterId("definitely-not-exists"))

        assertAll(
            {
                assertNull(nonExistentDelete) {
                    "Delete operation should return null for non-existent encounter entity, " +
                            "but returned: $nonExistentDelete"
                }
            }
        )
    }

    @Test
    @DisplayName("Complete CRUD operations workflow test for encounter entities")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("encounter test")
    fun completeEncounterCrudWorkflow() {
        logger.trace("Testing complete CRUD workflow for encounter entities")

        // Create and save multiple entities
        val entity1 = EncounterEntity.of(
            EncounterFactory.of(
                id = "1",
                patientReference = "Patient/111",
                encounterClass = "AMB",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "408",
                serviceTypeDisplay = "General Medicine"
            )
        )
        val entity2 = EncounterEntity.of(
            EncounterFactory.of(
                id = "2",
                patientReference = "Patient/222",
                encounterClass = "IMP",
                status = "in-progress",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "394802001",
                serviceTypeDisplay = "Cardiology"
            )
        )
        val entity3 = EncounterEntity.of(
            EncounterFactory.of(
                id = "3",
                patientReference = "Patient/333",
                encounterClass = "EMER",
                status = "finished",
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "773568002",
                serviceTypeDisplay = "Emergency Medicine"
            )
        )

        encounterRepository.save(entity1)
        encounterRepository.save(entity2)
        encounterRepository.save(entity3)

        // Verify all entities exist
        val allEntities = encounterRepository.findAll().toList()

        // Update one entity
        val updatedEntity2 = EncounterEntity.of(
            EncounterFactory.of(
                id = "2",
                patientReference = "Patient/222",
                encounterClass = "IMP",
                status = "finished", // Changed status
                serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
                serviceTypeCode = "394802001",
                serviceTypeDisplay = "Cardiology"
            )
        )
        encounterRepository.update(updatedEntity2)

        // Delete one entity
        val deletedEntity = encounterRepository.deleteById(EncounterId("1"))

        // Final state check
        val finalEntities = encounterRepository.findAll().toList()
        logger.trace("Final encounter entity count: ${finalEntities.size}")

        finalEntities.forEach { entity ->
            logger.trace("Remaining encounter entity: ${entity.id.value}")
        }

        val finalEntityIds = finalEntities.map { it.id.value }.toSet()

        assertAll(
            { assertTrue(allEntities.size >= 3) {
                "Expected at least 3 encounter entities after saving, found ${allEntities.size}"
            }
            },
            { assertNotNull(deletedEntity) { "Failed to delete encounter entity 1" } },
            { assertFalse(finalEntityIds.contains("1")) { "Encounter entity 1 should have been deleted" } },
            { assertTrue(finalEntityIds.contains("2")) { "Encounter entity 2 should still exist" } },
            { assertTrue(finalEntityIds.contains("3")) { "Encounter entity 3 should still exist" } }
        )
    }
}