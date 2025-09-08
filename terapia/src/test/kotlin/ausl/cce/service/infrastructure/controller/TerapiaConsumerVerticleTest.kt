package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.CarePlanService
import ausl.cce.service.application.CarePlanServiceImpl
import ausl.cce.service.application.EventProducerVerticle
import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.fromJsonToAllergyIntolerance
import ausl.cce.service.domain.fromJsonToCarePlan
import ausl.cce.service.domain.toJson
import ausl.cce.service.application.CarePlanRepository
import ausl.cce.service.infrastructure.persistence.MongoCarePlanRepository
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.mockk
import io.mockk.spyk
import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.Vertx.vertx
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import mf.cce.utils.AllergyDiagnosed
import mf.cce.utils.DockerTest
import mf.cce.utils.DomainEvent
import mf.cce.utils.RepositoryCredentials
import mf.cce.utils.TherapyRevoked
import mf.cce.utils.allergyIntoleranceTest
import mf.cce.utils.carePlanTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class TerapiaConsumerVerticleTest : DockerTest() {
    private val logger = LogManager.getLogger(this::class)
    private val dockerComposePath = "/ausl/cce/service/infrastructure/controller/eventBrokerDeploy.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var terapiaEventProducer: TerapiaProducerVerticle
    private lateinit var anamnesiTestEventProducer: AnamnesiProducerVerticle
    private lateinit var carePlanRepository: CarePlanRepository
    private lateinit var carePlanService: CarePlanService
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
        carePlanRepository = MongoCarePlanRepository(repositoryCredentials)
        terapiaEventProducer = spyk(TerapiaProducerVerticle())
        anamnesiTestEventProducer = spyk(AnamnesiProducerVerticle())
        val mockMeterRegistry = mockk<MeterRegistry>(relaxed = true)   // 'dummy' test double
        val serviceName = "terapia-test"
        carePlanService = spyk(CarePlanServiceImpl(carePlanRepository, terapiaEventProducer, mockMeterRegistry, serviceName))

        vertx = vertx()

        deployVerticle(vertx, terapiaEventProducer, anamnesiTestEventProducer)
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
            carePlanRepository.close()
        } catch (e: Exception) {
            logger.warn("Error closing repository: ${e.message}")
        }
        executeDockerComposeDown(dockerComposeFile)
    }

    @Test
    @DisplayName("Test event consumption: AllergyDiagnosed")
    @Timeout(5 * 60) // 5 minutes timeout
    fun testEventConsumption() {
        val latch = CountDownLatch(1)
        var received = false

        carePlanRepository.save(CarePlanEntity.of(carePlanTest.fromJsonToCarePlan()))

        // Publish an AllergyDiagnosed event
        anamnesiTestEventProducer.publishEvent(AllergyDiagnosed.of(allergyIntoleranceTest.fromJsonToAllergyIntolerance()))

        // the published event will be consumed by the 'TerapiaConsumerVerticle' and forwarded to the event bus from the
        // 'CarePlanService', so we can listen to it here to verify that it was processed
        vertx.eventBus().consumer<String>(TherapyRevoked.TOPIC) {
            received = true
            latch.countDown()
        }

        latch.await(4, TimeUnit.MINUTES)

        assertTrue(received)
    }
}

/**
 * Test class to produce 'AllergyDiagnosed' events
 */
class AnamnesiProducerVerticle : AbstractVerticle(), EventProducerVerticle {
    private val logger = LogManager.getLogger(this::class)
    private val producerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
        "acks" to "1"
    )
    private lateinit var producer: KafkaProducer<String, String>

    /**
     * Start the producer
     */
    override fun start() {
        producer = KafkaProducer.create(vertx, producerConfig)
    }

    /**
     * Publish an event to Kafka.
     * @param event the event to publish. Possible events are:
     * - AllergyDiagnosed
     */
    override fun publishEvent(event: DomainEvent) {
        when (event) {
            is AllergyDiagnosed -> publish(AllergyDiagnosed.TOPIC, event.allergyIntolerance.toJson())
        }
    }

    /**
     * Publish an event to Kafka
     * @param topic the topic to publish the event to
     * @param value the event to publish
     * @param key the key of the event
     */
    private fun publish(topic: String, value: String, key: String? = null) {
        val record = KafkaProducerRecord.create<String, String>(
            topic,
            key,
            value
        )
        producer.write(record)
        logger.trace("Published event: TOPIC:{}, KEY:{}, VALUE:{}", topic, key, value)
    }
}