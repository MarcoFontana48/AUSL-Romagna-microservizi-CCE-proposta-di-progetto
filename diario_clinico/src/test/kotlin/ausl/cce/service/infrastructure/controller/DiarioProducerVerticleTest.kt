package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.EncounterService
import ausl.cce.service.application.EncounterServiceImpl
import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterFactory
import ausl.cce.service.infrastructure.persistence.EncounterRepository
import ausl.cce.service.infrastructure.persistence.MongoEncounterRepository
import io.mockk.spyk
import io.mockk.verify
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.Vertx.vertx
import io.vertx.core.Verticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import mf.cce.utils.DockerTest
import mf.cce.utils.EncounterConcluded
import mf.cce.utils.RepositoryCredentials
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DiarioProducerVerticleTest : DockerTest() {
    val encounterTest = EncounterFactory.of(
        id = "123",
        patientReference = "Patient/123",
        encounterClass = "AMB",
        status = "finished",
        serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
        serviceTypeCode = "408",
        serviceTypeDisplay = "General Medicine"
    )
    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/controller/eventBrokerDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var encounterEventProducer: EncounterEventProducerVerticle
    private lateinit var encounterEventConsumer: ConsumerVerticleTestClass
    private lateinit var encounterService: EncounterService
    private lateinit var encounterRepository: EncounterRepository
    private lateinit var vertx: Vertx
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
        encounterRepository = MongoEncounterRepository(repositoryCredentials)
        encounterEventProducer = spyk(EncounterEventProducerVerticle())
        encounterEventConsumer = spyk(ConsumerVerticleTestClass())
        encounterService = EncounterServiceImpl(encounterRepository, encounterEventProducer)

        vertx = vertx()

        deployVerticle(vertx, encounterEventProducer, encounterEventConsumer)
    }

    private fun deployVerticle(vertx: Vertx, vararg verticles: Verticle) {
        val latch = CountDownLatch(verticles.size)
        logger.debug("verticles size: {}", verticles.size)
        verticles.forEach { verticle ->
            vertx.deployVerticle(verticle).onComplete {
                latch.countDown()
                if (it.succeeded()) {
                    logger.info("Verticle '{}' started", verticle.javaClass.simpleName)
                } else {
                    logger.error("Failed to start verticle '{}':", verticle.javaClass.simpleName, it.cause())
                }
            }
        }
        latch.await(4, TimeUnit.MINUTES)
    }

    @AfterEach
    fun tearDown() {
        try {
            encounterRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing repository: ${e.message}")
        }
        executeDockerComposeDown(dockerComposeFile)
    }

    @Test
    @DisplayName("Verify method publishEvent is called when an allergy is added")
    @Timeout(5 * 60) // 5 minutes timeout
    fun publishEventCalled() {
        // produce event
        encounterService.addEncounter(EncounterEntity.of(encounterTest))

        assertAll(
            { verify(exactly = 1) { encounterEventProducer.publishEvent(any()) } },
        )
    }

    @Test
    @DisplayName("Verify event EncounterConcluded is correctly produced when an allergy is added")
    @Timeout(5 * 60) // 5 minutes timeout
    fun produceEvent() {
        // produce event
        encounterService.addEncounter(EncounterEntity.of(encounterTest))

        // consume event (to verify it has been produced)
        val latch = CountDownLatch(1)
        var received = false
        vertx.eventBus().consumer<String>(EncounterConcluded.TOPIC) {
            logger.debug("Received event on topic {}: {}", EncounterConcluded.TOPIC, it.body())
            received = true
            latch.countDown()
        }
        latch.await(4, TimeUnit.MINUTES)

        assertAll(
            { assertTrue(received) },
        )
    }
}

/**
 * Class defined only to test events that this microservice produces, in order to test its producer.
 */
class ConsumerVerticleTestClass : AbstractVerticle() {
    private val logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "diario-clinico",
        "auto.offset.reset" to "earliest"
    )
    private val events: MutableSet<String> = mutableSetOf(
        EncounterConcluded.TOPIC,
    )
    private lateinit var consumer: KafkaConsumer<String, String>

    /**
     * Starts the Kafka consumer.
     */
    override fun start() {
        consumer = KafkaConsumer.create(vertx, consumerConfig)
        subscribeToEvents()
        handleEvents()
    }

    /**
     * Subscribes to the events that this microservice is interested in.
     */
    private fun subscribeToEvents() {
        consumer.subscribe(events) { result ->
            if (result.succeeded()) {
                logger.debug("Subscribed to events: {}", events)
            } else {
                logger.error("Failed to subscribe to events {}", events)
            }
        }
    }

    /**
     * Handles the events received from the Kafka consumer.
     */
    private fun handleEvents() {
        consumer.handler { record ->
            logger.trace("Received event: TOPIC:{}, KEY:{}, VALUE:{}", record.topic(), record.key(), record.value())
            when (record.topic()) {
                EncounterConcluded.TOPIC -> encounterConcludedHandler(record)
                else -> logger.warn("Received event from unknown topic: {}", record.topic())
            }
        }
    }

    /**
     * Simply forwards the event to the event bus to be able to check whether the test has been successful.
     */
    private fun encounterConcludedHandler(record: KafkaConsumerRecord<String, String>) {
        vertx.eventBus().publish(EncounterConcluded.TOPIC, record.value())
    }
}
