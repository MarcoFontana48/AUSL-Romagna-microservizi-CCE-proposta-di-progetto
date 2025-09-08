package ausl.cce.service.application

import io.vertx.core.Verticle
import mf.cce.utils.DomainEvent

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