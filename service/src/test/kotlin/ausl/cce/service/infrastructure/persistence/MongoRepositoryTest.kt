package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.domain.DummyEntity
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
    private val testEntity = DummyEntity.of(DummyEntity.DummyId("test-123"), "dummy-field")
    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/persistence/mongoDbDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var serviceRepository: MongoRepository

    val repositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "service-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "service-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeUp(dockerComposeFile)

        // connection is done here, if it fails, all tests will fail, so it can be tested here like this
        serviceRepository = MongoRepository(repositoryCredentials)
    }

    @AfterEach
    fun tearDown() {
        try {
            serviceRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing repository: ${e.message}")
        }
        executeDockerComposeDown(dockerComposeFile)
    }

    @Test
    @DisplayName("Test saving and finding an existing entity by its ID")
    @Timeout(5 * 60) // 5 minutes timeout
    fun saveAndFindById() {
        logger.trace("Saving entity with id: {}", testEntity)
        val expectedEntity = testEntity
        serviceRepository.save(testEntity)

        val foundEntity = serviceRepository.findById(DummyEntity.DummyId("test-123"))

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

        val foundEntity = serviceRepository.findById(nonExistentEntity.id)

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

        serviceRepository.save(testEntity1)
        serviceRepository.save(testEntity2)
        serviceRepository.save(testEntity3)

        logger.trace("Saved additional entities: test-456, test-789")

        val allEntities = serviceRepository.findAll().toList()
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
        serviceRepository.save(originalEntity)

        // Create updated entity with same ID
        val entityToUpdate = DummyEntity.of(DummyEntity.DummyId("test-123"), "updated-field")
        logger.trace("Updating entity with id: ${entityToUpdate.id.value}")

        // Update should not throw exception for existing entity
        serviceRepository.update(entityToUpdate)

        // Verify the entity still exists after update
        val updatedEntity = serviceRepository.findById(DummyEntity.DummyId("test-123"))

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
            serviceRepository.update(nonExistentEntity)
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
        serviceRepository.save(entityToDelete)

        // Delete the entity
        val deletedEntity = serviceRepository.deleteById(DummyEntity.DummyId("test-456"))

        // Verify the entity no longer exists
        val shouldBeNull = serviceRepository.findById(DummyEntity.DummyId("test-456"))

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

        val nonExistentDelete = serviceRepository.deleteById(DummyEntity.DummyId("definitely-not-exists"))

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

        serviceRepository.save(entity1)
        serviceRepository.save(entity2)
        serviceRepository.save(entity3)

        // Verify all entities exist
        val allEntities = serviceRepository.findAll().toList()

        // Update one entity
        val updatedEntity2 = DummyEntity.of(DummyEntity.DummyId("workflow-2"), "updated-field-2")
        serviceRepository.update(updatedEntity2)

        // Delete one entity
        val deletedEntity = serviceRepository.deleteById(DummyEntity.DummyId("workflow-1"))

        // Final state check
        val finalEntities = serviceRepository.findAll().toList()
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
}