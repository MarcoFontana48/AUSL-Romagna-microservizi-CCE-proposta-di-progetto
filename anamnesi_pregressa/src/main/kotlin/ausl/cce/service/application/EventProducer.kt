package ausl.cce.service.application

import ausl.cce.service.domain.toJson
import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import mf.cce.utils.AllergyDiagnosed
import mf.cce.utils.DomainEvent
import org.apache.logging.log4j.LogManager

/**
 * Interface for a verticle that produces events to the event broker
 */
interface EventProducerVerticle : Verticle {
    /**
     * Publish an event to the event broker
     * @param event the event to publish
     */
    fun publishEvent(event: DomainEvent)
}

/**
 * Verticle that produces events to Kafka
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