package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.application.AllergyIntoleranceRepository
import ausl.cce.service.application.DummyRepository
import ausl.cce.service.domain.AllergyIntoleranceEntity
import ausl.cce.service.domain.AllergyIntoleranceFactory
import ausl.cce.service.domain.AllergyIntoleranceId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.toJson
import mf.cce.utils.DockerTest
import mf.cce.utils.RepositoryCredentials
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.io.File

class MongoRepositoryTest : DockerTest() {
    private val dummyTestEntity = DummyEntity.of(DummyEntity.DummyId("test-123"), "dummy-field")
    val allergyIntoleranceTest = AllergyIntoleranceFactory.of(
        id = "123",
        patientReference = "Patient/123",
        allergenSystem = "http://snomed.info/sct",
        allergenCode = "227493005",
        allergenDisplay = "Cashew nuts",
        clinicalStatus = "active",
    )
    private val allergyTestWrapperEntity = AllergyIntoleranceEntity.of(allergyIntoleranceTest)
    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/persistence/mongoDbDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var dummyRepository: DummyRepository
    private lateinit var allergyIntoleranceRepository: AllergyIntoleranceRepository

    val repositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "anamnesi-pregressa-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "anamnesi-pregressa-mongo-db",
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
        allergyIntoleranceRepository = MongoAllergyIntoleranceRepository(repositoryCredentials)
    }

    @AfterEach
    fun tearDown() {
        try {
            dummyRepository.close()
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

    @Test
    @DisplayName("Test saving and finding an existing allergy entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun saveAndFindByAllergyId() {
        logger.trace("Saving entity with id: {}", allergyTestWrapperEntity)
        val expectedEntity = allergyTestWrapperEntity
        allergyIntoleranceRepository.save(allergyTestWrapperEntity)

        val foundEntity = allergyIntoleranceRepository.findById(AllergyIntoleranceId("123"))

        assertAll(
            { assertNotNull(foundEntity) {
                "Entity with id '123' not found after saving. " +
                        "This indicates a failure in the save operation or the findById method."
            }},
            { assertEquals(expectedEntity, foundEntity) {
                "Expected entity $expectedEntity, but found $foundEntity. " +
                        "This indicates a failure in the findById method."
            }},
            { assertEquals(allergyIntoleranceTest.toJson(), foundEntity?.allergyIntolerance?.toJson()) {
                "Expected FHIR resource ${allergyIntoleranceTest.toJson()}, but found ${foundEntity?.allergyIntolerance?.toJson()}. " +
                        "This indicates a failure in the findById method."
            }},
        )
    }

    @Test
    @DisplayName("Test finding a non-existent allergy entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun findByAllergyIdNonExistent() {
        val nonExistentEntity = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "123",
                patientReference = "Patient/999",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "000000",
                allergenDisplay = "Non-existent allergen",
                clinicalStatus = "inactive",
            )
        )
        val foundEntity = allergyIntoleranceRepository.findById(nonExistentEntity.id)
        assertAll(
            {
                assertNull(foundEntity) {
                    "Expected no entity to be found for ID '${nonExistentEntity.id.value}', " +
                            "but found: ${foundEntity?.id?.value}. " +
                            "This indicates a failure in the findById method, as it should return null for non-existent IDs."
                }
            }
        )
    }

    @Test
    @DisplayName("Test finding all allergy entities")
    @Timeout(5 * 60) // 5 minutes timeout
    fun findAllAllergyEntities() {
        logger.trace("Testing findAll functionality for allergy entities")
        // Save multiple test entities
        val testEntity1 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "123",
                patientReference = "Patient/123",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493005",
                allergenDisplay = "Cashew nuts",
                clinicalStatus = "active",
            )
        )
        val testEntity2 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "456",
                patientReference = "Patient/456",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493006",
                allergenDisplay = "Peanuts",
                clinicalStatus = "active",
            )
        )

        val testEntity3 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "789",
                patientReference = "Patient/789",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493007",
                allergenDisplay = "Tree nuts",
                clinicalStatus = "active",
            )
        )

        allergyIntoleranceRepository.save(testEntity1)
        allergyIntoleranceRepository.save(testEntity2)
        allergyIntoleranceRepository.save(testEntity3)

        logger.trace("Saved additional allergy entities: allergy-456, allergy-789")

        val allEntities = allergyIntoleranceRepository.findAll().toList()
        logger.trace("Found ${allEntities.size} allergy entities")

        allEntities.forEach { entity ->
            logger.trace("Allergy Entity ID: ${entity.id.value}")
        }

        val entityIds = allEntities.map { it.id.value }.toSet()

        assertAll(
            {
                assertTrue(allEntities.size >= 3) {
                    "FindAll test failed - expected at least 3 entities, found ${allEntities.size}. " +
                            "This indicates a failure in the findAll or save methods."
                }
            },
            { assertTrue(entityIds.contains("123")) { "Entity 123 not found in findAll results" } },
            { assertTrue(entityIds.contains("456")) { "Entity 456 not found in findAll results" } },
            { assertTrue(entityIds.contains("789")) { "Entity 789 not found in findAll results" } }
        )
    }

    @Test
    @DisplayName("Test updating an existing allergy entity")
    @Timeout(5 * 60) // 5 minutes timeout
    fun updateExistingAllergyEntity() {
        logger.trace("Testing update functionality for existing allergy entity")
        // First save an entity
        val originalEntity = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "123",
                patientReference = "Patient/123",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493005",
                allergenDisplay = "Cashew nuts",
                clinicalStatus = "active",
            )
        )
        allergyIntoleranceRepository.save(originalEntity)
        // Create updated entity with same ID
        val updatedEntity = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "123",
                patientReference = "Patient/123",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "762952008",
                allergenDisplay = "Peanuts",
                clinicalStatus = "inactive",
            )
        )
        logger.trace("Updating allergy entity with id: ${updatedEntity.id.value}")

        // Update should not throw exception for existing entity
        allergyIntoleranceRepository.update(updatedEntity)

        // Verify the entity still exists after update
        val updatedEntityFromDb = allergyIntoleranceRepository.findById(AllergyIntoleranceId("123"))

        assertAll(
            {
                assertNotNull(updatedEntityFromDb) {
                    "Allergy entity not found after update. This indicates a failure in the update method."
                }
            },
            {
                assertEquals(updatedEntity, updatedEntityFromDb) {
                    "Updated allergy entity does not match expected entity"
                }
            }
        )
    }

    @Test
    @DisplayName("Test updating a non-existent allergy entity should throw exception")
    @Timeout(5 * 60) // 5 minutes timeout
    fun updateNonExistentAllergyEntity() {
        logger.trace("Testing update functionality for non-existent allergy entity")
        val nonExistentEntity = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "non-existent-update",
                patientReference = "Patient/999",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "000000",
                allergenDisplay = "Non-existent allergen",
                clinicalStatus = "inactive",
            )
        )
        // Update should throw RuntimeException for non-existent entity
        val exception = assertThrows<RuntimeException> {
            allergyIntoleranceRepository.update(nonExistentEntity)
        }
        logger.trace("Correctly threw exception for non-existent allergy entity update: ${exception.message}")
        assertAll(
            {
                assertNotNull(exception) {
                    "Expected RuntimeException to be thrown for non-existent allergy entity update"
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
    @DisplayName("Test deleting an existing allergy entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun deleteExistingAllergyEntityById() {
        logger.trace("Testing deleteById functionality for existing allergy entity")
        // First save entities to delete
        val entityToDelete = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "456",
                patientReference = "Patient/456",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493006",
                allergenDisplay = "Peanuts",
                clinicalStatus = "active",
            )
        )
        allergyIntoleranceRepository.save(entityToDelete)
        // Delete the entity
        val deletedEntity = allergyIntoleranceRepository.deleteById(AllergyIntoleranceId("456"))
        // Verify the entity no longer exists
        val shouldBeNull = allergyIntoleranceRepository.findById(AllergyIntoleranceId("456"))
        assertAll(
            {
                assertNotNull(deletedEntity) {
                    "Delete operation should return the deleted allergy entity, but returned null"
                }
            },
            {
                assertEquals(entityToDelete, deletedEntity) {
                    "Deleted allergy entity does not match the expected entity"
                }
            },
            {
                assertNull(shouldBeNull) {
                    "Allergy entity still exists after deletion. This indicates a failure in the deleteById method."
                }
            }
        )
    }

    @Test
    @DisplayName("Test deleting a non-existent allergy entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun deleteNonExistentAllergyEntityById() {
        logger.trace("Testing deleteById functionality for non-existent allergy entity")
        val nonExistentDelete = allergyIntoleranceRepository.deleteById(AllergyIntoleranceId("definitely-not-exists"))
        assertAll(
            {
                assertNull(nonExistentDelete) {
                    "Delete operation should return null for non-existent allergy entity, " +
                            "but returned: $nonExistentDelete"
                }
            }
        )
    }

    @Test
    @DisplayName("Complete CRUD operations workflow test for allergy entities")
    @Timeout(5 * 60) // 5 minutes timeout
    fun completeAllergyCrudWorkflow() {
        logger.trace("Testing complete CRUD workflow for allergy entities")
        // Create and save multiple entities
        val entity1 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "1",
                patientReference = "Patient/111",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493001",
                allergenDisplay = "Allergen 1",
                clinicalStatus = "active",
            )
        )
        val entity2 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "2",
                patientReference = "Patient/222",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493002",
                allergenDisplay = "Allergen 2",
                clinicalStatus = "active",
            )
        )
        val entity3 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "3",
                patientReference = "Patient/333",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "227493003",
                allergenDisplay = "Allergen 3",
                clinicalStatus = "active",
            )
        )
        allergyIntoleranceRepository.save(entity1)
        allergyIntoleranceRepository.save(entity2)
        allergyIntoleranceRepository.save(entity3)
        // Verify all entities exist
        val allEntities = allergyIntoleranceRepository.findAll().toList()
        // Update one entity
        val updatedEntity2 = AllergyIntoleranceEntity.of(
            AllergyIntoleranceFactory.of(
                id = "2",
                patientReference = "Patient/222",
                allergenSystem = "http://snomed.info/sct",
                allergenCode = "762952008",
                allergenDisplay = "Peanuts",
                clinicalStatus = "inactive",
            )
        )
        allergyIntoleranceRepository.update(updatedEntity2)
        // Delete one entity
        val deletedEntity = allergyIntoleranceRepository.deleteById(AllergyIntoleranceId("1"))
        // Final state check
        val finalEntities = allergyIntoleranceRepository.findAll().toList()
        logger.trace("Final allergy entity count: ${finalEntities.size}")
        finalEntities.forEach { entity ->
            logger.trace("Remaining allergy entity: ${entity.id.value}")
        }
        val finalEntityIds = finalEntities.map { it.id.value }.toSet()
        assertAll(
            { assertTrue(allEntities.size >= 3) {
                    "Expected at least 3 allergy entities after saving, found ${allEntities.size}"
                }
            },
            { assertNotNull(deletedEntity) { "Failed to delete allergy entity 1" } },
            { assertFalse(finalEntityIds.contains("1")) { "Allergy entity 1 should have been deleted" } },
            { assertTrue(finalEntityIds.contains("2")) { "Allergy entity 2 should still exist" } },
            { assertTrue(finalEntityIds.contains("3")) { "Allergy entity 3 should still exist" } }
        )
    }
}