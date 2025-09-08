package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.AllergyIntoleranceEventHandler
import ausl.cce.service.application.CarePlanService
import ausl.cce.service.application.EventProducerVerticle
import ausl.cce.service.domain.fromJsonToAllergyIntolerance
import ausl.cce.service.domain.toJson
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import mf.cce.utils.AllergyDiagnosed
import mf.cce.utils.DomainEvent
import mf.cce.utils.TherapyRevoked
import org.apache.logging.log4j.LogManager

/**
 * Consumer verticle for the Terapia microservice to listen to events from the event broker
 */
class TerapiaConsumerVerticle(
    private val service: CarePlanService
) : AbstractVerticle(), AllergyIntoleranceEventHandler {
    private val logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "terapia",
        "auto.offset.reset" to "earliest"
    )
    private val events: MutableSet<String> = mutableSetOf(
        AllergyDiagnosed.TOPIC,
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
                logger.info("Subscribed to events: {}", events)
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
                AllergyDiagnosed.TOPIC -> allergyDiagnosedHandler(AllergyDiagnosed.of(record.value().fromJsonToAllergyIntolerance()))
                else -> logger.warn("Received event from unknown topic: {}", record.topic())
            }
        }
    }

    /**
     * Forwards the event to the event bus to be able to process it.
     */
    override fun allergyDiagnosedHandler(event: AllergyDiagnosed) {
        service.checkAndSuspendCarePlanIfConflict(event)
    }
}

/**
 * Verticle that produces events to Kafka
 */
class TerapiaProducerVerticle : AbstractVerticle(), EventProducerVerticle {
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
            is TherapyRevoked -> publish(TherapyRevoked.TOPIC, event.carePlan.toJson())
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