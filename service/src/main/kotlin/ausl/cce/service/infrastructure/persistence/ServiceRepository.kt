package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.application.DummyRepository
import ausl.cce.service.domain.DummyEntity
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import mf.cce.utils.RepositoryCredentials
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.Document

/**
 * MongoDB implementation of the DummyRepository.
 */
class MongoRepository(
    private val credentials: RepositoryCredentials
) : DummyRepository {
    private val logger: Logger = LogManager.getLogger(this::class)
    private val entityCollection: MongoCollection<Document>
    private val mongoClient: MongoClient

    init {
        logger.trace("Connecting to '${credentials.host}:${credentials.port}:${credentials.dbName}' with user '${credentials.username}' and password '${credentials.password}'")

        try {
            this.mongoClient = MongoClients.create("mongodb://${credentials.username}:${credentials.password}@${credentials.host}:${credentials.port}")
            val db: MongoDatabase = mongoClient.getDatabase(credentials.dbName) ?: run {
                mongoClient.close()
                throw IllegalStateException("Database ${credentials.dbName} does not exist")
            }

            this.entityCollection = db.getCollection("dummy") ?: run {
                mongoClient.close()
                throw IllegalStateException("Collection 'dummy' does not exist in database ${credentials.dbName}")
            }
            logger.info("Successfully connected to MongoDB at ${credentials.host}:${credentials.port}, database: ${credentials.dbName}")

        } catch (e: Exception) {
            logger.error("Failed to connect to MongoDB: ${e.message}")
            throw IllegalStateException("MongoDB connection failed", e)
        }
    }

    /**
     * Saves a DummyEntity to the MongoDB collection.
     */
    override fun save(entity: DummyEntity) {
        try {
            logger.debug("Saving entity with id: ${entity.id.value}")

            val document = Document().apply {
                append("_id", entity.id.value)
                append("entityId", entity.id.value)
                append("field", entity.dummyField)
                append("createdAt", System.currentTimeMillis())
            }

            entityCollection.insertOne(document)
            logger.trace("Successfully saved entity with id: ${entity.id.value}")

        } catch (e: Exception) {
            logger.error("Failed to save entity with id: ${entity.id.value}. Error: ${e.message}")
            throw RuntimeException("Failed to save entity", e)
        }
    }

    /**
     * Finds a DummyEntity by its ID in the MongoDB collection.
     */
    override fun findById(id: DummyEntity.DummyId): DummyEntity? {
        return try {
            logger.debug("Finding entity with id: ${id.value}")

            val filter = Filters.eq("_id", id.value)
            val document = entityCollection.find(filter).first()

            if (document != null) {
                logger.debug("Found entity with id: ${id.value}")
                val entityId = document.getString("entityId") ?: document.getString("_id")
                val field = document.getString("field")
                DummyEntity.of(DummyEntity.DummyId(entityId), field)
            } else {
                logger.debug("No entity found with id: ${id.value}")
                null
            }

        } catch (e: Exception) {
            logger.error("Failed to find entity with id: ${id.value}. Error: ${e.message}")
            throw RuntimeException("Failed to find entity", e)
        }
    }

    /**
     * Deletes a DummyEntity by its ID from the MongoDB collection.
     */
    override fun deleteById(id: DummyEntity.DummyId): DummyEntity? {
        return try {
            logger.debug("Deleting entity with id: ${id.value}")

            val filter = Filters.eq("_id", id.value)

            val document = entityCollection.find(filter).first()

            if (document != null) {
                val deleteResult = entityCollection.deleteOne(filter)

                if (deleteResult.deletedCount > 0) {
                    logger.trace("Successfully deleted entity with id: ${id.value}")
                    val entityId = document.getString("entityId") ?: document.getString("_id")
                    val field = document.getString("field")
                    DummyEntity.of(DummyEntity.DummyId(entityId), field)
                } else {
                    logger.warn("Delete operation did not remove any documents for id: ${id.value}")
                    null
                }
            } else {
                logger.debug("No entity found to delete with id: ${id.value}")
                null
            }

        } catch (e: Exception) {
            logger.error("Failed to delete entity with id: ${id.value}. Error: ${e.message}")
            throw RuntimeException("Failed to delete entity", e)
        }
    }

    /**
     * Finds all DummyEntities in the MongoDB collection.
     */
    override fun findAll(): Iterable<DummyEntity> {
        return try {
            logger.debug("Finding all entities")

            val documents = entityCollection.find().toList()
            val entities = documents.mapNotNull { document ->
                try {
                    val entityId = document.getString("entityId") ?: document.getString("_id")
                    if (entityId != null) {
                        val field = document.getString("field")
                        DummyEntity.of(DummyEntity.DummyId(entityId), field)
                    } else {
                        logger.warn("Document found with null entityId: ${document.toJson()}")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Failed to convert document to entity: ${e.message}")
                    null
                }
            }

            logger.trace("Found ${entities.size} entities")
            entities

        } catch (e: Exception) {
            logger.error("Failed to find all entities. Error: ${e.message}")
            throw RuntimeException("Failed to find all entities", e)
        }
    }

    /**
     * Updates a DummyEntity in the MongoDB collection.
     */
    override fun update(entity: DummyEntity) {
        try {
            logger.debug("Updating entity with id: ${entity.id.value}")

            val filter = Filters.eq("_id", entity.id.value)
            val update = Updates.combine(
                Updates.set("entityId", entity.id.value),
                Updates.set("updatedAt", System.currentTimeMillis())
            )

            val updateResult = entityCollection.updateOne(filter, update)

            if (updateResult.matchedCount > 0) {
                logger.trace("Successfully updated entity with id: ${entity.id.value}")
            } else {
                logger.warn("No entity found to update with id: ${entity.id.value}")
                throw RuntimeException("Entity not found for update with id: ${entity.id.value}")
            }

        } catch (e: Exception) {
            logger.error("Failed to update entity with id: ${entity.id.value}. Error: ${e.message}")
            throw RuntimeException("Failed to update entity", e)
        }
    }

    /**
     * Closes the MongoDB client connection.
     */
    override fun close() {
        try {
            mongoClient.close()
            logger.info("MongoDB connection closed")
        } catch (e: Exception) {
            logger.warn("Error closing MongoDB connection: ${e.message}")
        }
    }
}