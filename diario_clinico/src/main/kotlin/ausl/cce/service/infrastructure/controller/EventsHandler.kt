package ausl.cce.service.infrastructure.controller

import ausl.cce.service.domain.toJson
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import mf.cce.utils.DomainEvent
import mf.cce.utils.EncounterConcluded
import org.apache.logging.log4j.LogManager
import ausl.cce.service.application.EventProducerVerticle

/**
 * Verticle that produces events to Kafka
 */
class EncounterEventProducerVerticle : AbstractVerticle(), EventProducerVerticle {
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
     * - EncounterConcluded
     */
    override fun publishEvent(event: DomainEvent) {
        when (event) {
            is EncounterConcluded -> publish(EncounterConcluded.TOPIC, event.encounter.toJson())
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