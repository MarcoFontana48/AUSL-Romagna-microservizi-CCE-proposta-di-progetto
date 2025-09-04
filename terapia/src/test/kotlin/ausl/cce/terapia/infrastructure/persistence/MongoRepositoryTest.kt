package ausl.cce.terapia.infrastructure.persistence

import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.CarePlanFactory
import ausl.cce.service.domain.CarePlanId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.toJson
import ausl.cce.service.infrastructure.persistence.CarePlanRepository
import ausl.cce.service.infrastructure.persistence.MongoCarePlanRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
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

class MongoRepositoryTest : DockerTest() {
    private val dummyTestEntity = DummyEntity.of(DummyEntity.DummyId("test-123"), "dummy-field")
    val carePlanTest = CarePlanFactory.of(
        id = "123",
        patientReference = "Patient/123",
        title = "Diabetes Management Plan",
        description = "Comprehensive plan for managing type 2 diabetes",
        status = "active",
        intent = "plan"
    )
    private val carePlanTestWrapperEntity = CarePlanEntity.of(carePlanTest)
    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/persistence/mongoDbDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var dummyRepository: MongoDummyRepository
    private lateinit var carePlanRepository: CarePlanRepository

    val repositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "terapia-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "terapia-mongo-db",
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
        carePlanRepository = MongoCarePlanRepository(repositoryCredentials)
    }

    @AfterEach
    fun tearDown() {
        try {
            dummyRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing dummy repository: ${e.message}")
        }
        try {
            carePlanRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing care plan repository: ${e.message}")
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
    @DisplayName("Test saving and finding an existing care plan entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun saveAndFindByCarePlanId() {
        logger.trace("Saving care plan entity with id: {}", carePlanTestWrapperEntity)
        val expectedEntity = carePlanTestWrapperEntity
        carePlanRepository.save(carePlanTestWrapperEntity)

        val foundEntity = carePlanRepository.findById(CarePlanId("123"))

        assertAll(
            { assertNotNull(foundEntity) {
                "Care plan entity with id '123' not found after saving. " +
                        "This indicates a failure in the save operation or the findById method."
            }},
            { assertEquals(expectedEntity, foundEntity) {
                "Expected care plan entity $expectedEntity, but found $foundEntity. " +
                        "This indicates a failure in the findById method."
            }},
            { assertEquals(carePlanTest.toJson(), foundEntity?.carePlan?.toJson()) {
                "Expected FHIR resource ${carePlanTest.toJson()}, but found ${foundEntity?.carePlan?.toJson()}. " +
                        "This indicates a failure in the findById method."
            }},
        )
    }

    @Test
    @DisplayName("Test finding a non-existent care plan entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun findByCarePlanIdNonExistent() {
        val nonExistentEntity = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "999",
                patientReference = "Patient/999",
                title = "Non-existent Plan",
                description = "This plan should not exist",
                status = "unknown",
                intent = "proposal"
            )
        )
        val foundEntity = carePlanRepository.findById(nonExistentEntity.id)
        assertAll(
            {
                assertNull(foundEntity) {
                    "Expected no care plan entity to be found for ID '${nonExistentEntity.id.value}', " +
                            "but found: ${foundEntity?.id?.value}. " +
                            "This indicates a failure in the findById method, as it should return null for non-existent IDs."
                }
            }
        )
    }

    @Test
    @DisplayName("Test finding all care plan entities")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun findAllCarePlanEntities() {
        logger.trace("Testing findAll functionality for care plan entities")
        // Save multiple test entities
        val testEntity1 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "123",
                patientReference = "Patient/123",
                title = "Diabetes Management Plan",
                description = "Comprehensive diabetes care",
                status = "active",
                intent = "plan"
            )
        )
        val testEntity2 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "456",
                patientReference = "Patient/456",
                title = "Hypertension Management Plan",
                description = "Blood pressure control plan",
                status = "active",
                intent = "plan"
            )
        )

        val testEntity3 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "789",
                patientReference = "Patient/789",
                title = "Weight Management Plan",
                description = "Obesity treatment plan",
                status = "active",
                intent = "plan"
            )
        )

        carePlanRepository.save(testEntity1)
        carePlanRepository.save(testEntity2)
        carePlanRepository.save(testEntity3)

        logger.trace("Saved additional care plan entities: careplan-456, careplan-789")

        val allEntities = carePlanRepository.findAll().toList()
        logger.trace("Found ${allEntities.size} care plan entities")

        allEntities.forEach { entity ->
            logger.trace("Care Plan Entity ID: ${entity.id.value}")
        }

        val entityIds = allEntities.map { it.id.value }.toSet()

        assertAll(
            {
                assertTrue(allEntities.size >= 3) {
                    "FindAll test failed - expected at least 3 care plan entities, found ${allEntities.size}. " +
                            "This indicates a failure in the findAll or save methods."
                }
            },
            { assertTrue(entityIds.contains("123")) { "Care plan entity 123 not found in findAll results" } },
            { assertTrue(entityIds.contains("456")) { "Care plan entity 456 not found in findAll results" } },
            { assertTrue(entityIds.contains("789")) { "Care plan entity 789 not found in findAll results" } }
        )
    }

    @Test
    @DisplayName("Test updating an existing care plan entity")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun updateExistingCarePlanEntity() {
        logger.trace("Testing update functionality for existing care plan entity")
        // First save an entity
        val originalEntity = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "123",
                patientReference = "Patient/123",
                title = "Original Diabetes Plan",
                description = "Original diabetes management",
                status = "active",
                intent = "plan"
            )
        )
        carePlanRepository.save(originalEntity)
        // Create updated entity with same ID
        val updatedEntity = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "123",
                patientReference = "Patient/123",
                title = "Updated Diabetes Plan",
                description = "Enhanced diabetes management with new protocols",
                status = "active",
                intent = "plan"
            )
        )
        logger.trace("Updating care plan entity with id: ${updatedEntity.id.value}")

        // Update should not throw exception for existing entity
        carePlanRepository.update(updatedEntity)

        // Verify the entity still exists after update
        val updatedEntityFromDb = carePlanRepository.findById(CarePlanId("123"))

        assertAll(
            {
                assertNotNull(updatedEntityFromDb) {
                    "Care plan entity not found after update. This indicates a failure in the update method."
                }
            },
            {
                assertEquals(updatedEntity, updatedEntityFromDb) {
                    "Updated care plan entity does not match expected entity"
                }
            }
        )
    }

    @Test
    @DisplayName("Test updating a non-existent care plan entity should throw exception")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun updateNonExistentCarePlanEntity() {
        logger.trace("Testing update functionality for non-existent care plan entity")
        val nonExistentEntity = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "non-existent-update",
                patientReference = "Patient/999",
                title = "Non-existent Plan",
                description = "This plan should not exist",
                status = "unknown",
                intent = "proposal"
            )
        )
        // Update should throw RuntimeException for non-existent entity
        val exception = assertThrows<RuntimeException> {
            carePlanRepository.update(nonExistentEntity)
        }
        logger.trace("Correctly threw exception for non-existent care plan entity update: ${exception.message}")
        assertAll(
            {
                assertNotNull(exception) {
                    "Expected RuntimeException to be thrown for non-existent care plan entity update"
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
    @DisplayName("Test deleting an existing care plan entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun deleteExistingCarePlanEntityById() {
        logger.trace("Testing deleteById functionality for existing care plan entity")
        // First save entities to delete
        val entityToDelete = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "456",
                patientReference = "Patient/456",
                title = "Hypertension Management Plan",
                description = "Blood pressure control plan",
                status = "active",
                intent = "plan"
            )
        )
        carePlanRepository.save(entityToDelete)
        // Delete the entity
        val deletedEntity = carePlanRepository.deleteById(CarePlanId("456"))
        // Verify the entity no longer exists
        val shouldBeNull = carePlanRepository.findById(CarePlanId("456"))
        assertAll(
            {
                assertNotNull(deletedEntity) {
                    "Delete operation should return the deleted care plan entity, but returned null"
                }
            },
            {
                assertEquals(entityToDelete, deletedEntity) {
                    "Deleted care plan entity does not match the expected entity"
                }
            },
            {
                assertNull(shouldBeNull) {
                    "Care plan entity still exists after deletion. This indicates a failure in the deleteById method."
                }
            }
        )
    }

    @Test
    @DisplayName("Test deleting a non-existent care plan entity by ID")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun deleteNonExistentCarePlanEntityById() {
        logger.trace("Testing deleteById functionality for non-existent care plan entity")
        val nonExistentDelete = carePlanRepository.deleteById(CarePlanId("definitely-not-exists"))
        assertAll(
            {
                assertNull(nonExistentDelete) {
                    "Delete operation should return null for non-existent care plan entity, " +
                            "but returned: $nonExistentDelete"
                }
            }
        )
    }

    @Test
    @DisplayName("Complete CRUD operations workflow test for care plan entities")
    @Timeout(5 * 60) // 5 minutes timeout
    @Tag("careplan test")
    fun completeCarePlanCrudWorkflow() {
        logger.trace("Testing complete CRUD workflow for care plan entities")
        // Create and save multiple entities
        val entity1 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "1",
                patientReference = "Patient/111",
                title = "Diabetes Care Plan",
                description = "Comprehensive diabetes management",
                status = "active",
                intent = "plan"
            )
        )
        val entity2 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "2",
                patientReference = "Patient/222",
                title = "Cardiac Rehabilitation Plan",
                description = "Post-surgery cardiac recovery",
                status = "active",
                intent = "plan"
            )
        )
        val entity3 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "3",
                patientReference = "Patient/333",
                title = "Mental Health Support Plan",
                description = "Depression and anxiety management",
                status = "active",
                intent = "plan"
            )
        )
        carePlanRepository.save(entity1)
        carePlanRepository.save(entity2)
        carePlanRepository.save(entity3)
        // Verify all entities exist
        val allEntities = carePlanRepository.findAll().toList()
        // Update one entity
        val updatedEntity2 = CarePlanEntity.of(
            CarePlanFactory.of(
                id = "2",
                patientReference = "Patient/222",
                title = "Enhanced Cardiac Rehabilitation Plan",
                description = "Extended post-surgery cardiac recovery with physiotherapy",
                status = "active",
                intent = "plan"
            )
        )
        carePlanRepository.update(updatedEntity2)
        // Delete one entity
        val deletedEntity = carePlanRepository.deleteById(CarePlanId("1"))
        // Final state check
        val finalEntities = carePlanRepository.findAll().toList()
        logger.trace("Final care plan entity count: ${finalEntities.size}")
        finalEntities.forEach { entity ->
            logger.trace("Remaining care plan entity: ${entity.id.value}")
        }
        val finalEntityIds = finalEntities.map { it.id.value }.toSet()
        assertAll(
            { assertTrue(allEntities.size >= 3) {
                "Expected at least 3 care plan entities after saving, found ${allEntities.size}"
            }
            },
            { assertNotNull(deletedEntity) { "Failed to delete care plan entity 1" } },
            { assertFalse(finalEntityIds.contains("1")) { "Care plan entity 1 should have been deleted" } },
            { assertTrue(finalEntityIds.contains("2")) { "Care plan entity 2 should still exist" } },
            { assertTrue(finalEntityIds.contains("3")) { "Care plan entity 3 should still exist" } }
        )
    }
}