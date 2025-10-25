package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.application.DummyRepository
import ausl.cce.service.application.EncounterRepository
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterId
import ausl.cce.service.domain.fromJsonToEncounter
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import mf.cce.utils.Entity
import mf.cce.utils.ID
import mf.cce.utils.Repository
import mf.cce.utils.RepositoryCredentials
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.Document

/**
 * Generic MongoDB repository that can work with any Entity type
 *
 * @param I ID type extending ID<*>
 * @param E Entity type extending Entity<I>
 * @param credentials MongoDB connection credentials
 * @param collectionName Name of the MongoDB collection
 * @param entitySerializer Function to serialize entity to Document
 * @param entityDeserializer Function to deserialize Document to entity
 */
abstract class MongoRepository<I : ID<*>, E : Entity<I>>(
    private val credentials: RepositoryCredentials,
    private val collectionName: String,
    private val entitySerializer: (E) -> Document,
    private val entityDeserializer: (Document) -> E?
) : Repository<I, E> {

    private val logger: Logger = LogManager.getLogger(this::class)
    protected val entityCollection: MongoCollection<Document>
    private val mongoClient: MongoClient

    init {
        logger.trace("Connecting to '${credentials.host}:${credentials.port}:${credentials.dbName}' with user '${credentials.username}' and password '${credentials.password}'")

        try {
            this.mongoClient = MongoClients.create("mongodb://${credentials.username}:${credentials.password}@${credentials.host}:${credentials.port}")
            val db: MongoDatabase = mongoClient.getDatabase(credentials.dbName) ?: run {
                mongoClient.close()
                throw IllegalStateException("Database ${credentials.dbName} does not exist")
            }

            this.entityCollection = db.getCollection(collectionName) ?: run {
                mongoClient.close()
                throw IllegalStateException("Collection '$collectionName' does not exist in database ${credentials.dbName}")
            }
            logger.info("Successfully connected to MongoDB at ${credentials.host}:${credentials.port}, database: ${credentials.dbName}, collection: $collectionName")

        } catch (e: Exception) {
            logger.error("Failed to connect to MongoDB: ${e.message}")
            throw IllegalStateException("MongoDB connection failed", e)
        }
    }

    /**
     * Saves a new entity to the MongoDB collection.
     *
     * @param entity The entity to save.
     * @throws RuntimeException if the save operation fails.
     */
    override fun save(entity: E) {
        try {
            logger.debug("Saving entity with id: ${entity.id.value}")

            val document = entitySerializer(entity).apply {
                append("_id", entity.id.value)
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
     * Finds an entity by its ID in the MongoDB collection.
     *
     * @param id The ID of the entity to find.
     * @return The found entity, or null if not found.
     * @throws RuntimeException if the find operation fails.
     */
    override fun findById(id: I): E? {
        return try {
            logger.debug("Finding entity with id: ${id.value}")

            val filter = Filters.eq("_id", id.value)
            val document = entityCollection.find(filter).first()

            if (document != null) {
                logger.debug("Found entity with id: ${id.value}")
                entityDeserializer(document)
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
     * Deletes an entity by its ID from the MongoDB collection.
     *
     * @param id The ID of the entity to delete.
     * @return The deleted entity, or null if not found.
     * @throws RuntimeException if the delete operation fails.
     */
    override fun deleteById(id: I): E? {
        return try {
            logger.debug("Deleting entity with id: ${id.value}")

            val filter = Filters.eq("_id", id.value)
            val document = entityCollection.find(filter).first()

            if (document != null) {
                val deleteResult = entityCollection.deleteOne(filter)

                if (deleteResult.deletedCount > 0) {
                    logger.trace("Successfully deleted entity with id: ${id.value}")
                    entityDeserializer(document)
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
     * Finds all entities in the MongoDB collection.
     *
     * @return An iterable of all entities.
     * @throws RuntimeException if the find operation fails.
     */
    override fun findAll(): Iterable<E> {
        return try {
            logger.debug("Finding all entities")

            val documents = entityCollection.find().toList()
            val entities = documents.mapNotNull { document ->
                try {
                    entityDeserializer(document)
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
     * Updates an existing entity in the MongoDB collection.
     *
     * @param entity The entity to update.
     * @throws RuntimeException if the update operation fails.
     */
    override fun update(entity: E) {
        try {
            logger.debug("Updating entity with id: ${entity.id.value}")

            val filter = Filters.eq("_id", entity.id.value)

            // Get the serialized document and convert it to update operations
            val serializedDoc = entitySerializer(entity)
            val updates = serializedDoc.entries.map { (key, value) ->
                if (key != "_id") Updates.set(key, value) else null
            }.filterNotNull().toMutableList()

            // Add updatedAt timestamp
            updates.add(Updates.set("updatedAt", System.currentTimeMillis()))

            val combinedUpdate = Updates.combine(updates)
            val updateResult = entityCollection.updateOne(filter, combinedUpdate)

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
    fun close() {
        try {
            mongoClient.close()
            logger.info("MongoDB connection closed")
        } catch (e: Exception) {
            logger.warn("Error closing MongoDB connection: ${e.message}")
        }
    }
}

/**
 * Concrete implementation for DummyEntity
 */
class MongoDummyRepository(
    credentials: RepositoryCredentials
) : MongoRepository<DummyEntity.DummyId, DummyEntity>(
    credentials = credentials,
    collectionName = "dummy",
    entitySerializer = { entity ->
        Document().apply {
            append("entityId", entity.id.value)
            append("field", entity.dummyField)
        }
    },
    entityDeserializer = { document ->
        try {
            val entityId = document.getString("entityId") ?: document.getString("_id")
            val field = document.getString("field")
            if (entityId != null && field != null) {
                DummyEntity.of(DummyEntity.DummyId(entityId), field)
            } else null
        } catch (e: Exception) {
            null
        }
    }
), DummyRepository

/**
 * Concrete implementation for EncounterEntity
 */
class MongoEncounterRepository(
    credentials: RepositoryCredentials
) : MongoRepository<EncounterId, EncounterEntity>(
    credentials = credentials,
    collectionName = "encounter",
    entitySerializer = { entity ->
        Document().apply {
            append("entityId", entity.id.value)
            append("encounterJson", entity.toJson())
            append("patientReference", entity.encounter.subject?.reference)
            append("status", entity.encounter.status?.toCode())
            append("encounterClass", entity.encounter.class_?.code)
        }
    },
    entityDeserializer = { document ->
        try {
            val entityId = document.getString("entityId") ?: document.getString("_id")
            val encounterJson = document.getString("encounterJson")
            if (entityId != null && encounterJson != null) {
                val encounter = encounterJson.fromJsonToEncounter()
                EncounterEntity.of(EncounterId(entityId), encounter)
            } else null
        } catch (e: Exception) {
            // Log the exception if needed
            null
        }
    }
), EncounterRepository