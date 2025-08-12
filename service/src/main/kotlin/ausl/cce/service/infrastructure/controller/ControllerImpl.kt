package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.Controller
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.AbstractRegistryController
import mf.cce.utils.HttpStatus
import org.apache.logging.log4j.LogManager

class StandardController(
    private val circuitBreaker: CircuitBreaker,
    override val meterRegistry: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
) : Controller, AbstractRegistryController("service") {

    private val logger = LogManager.getLogger(this::class.java)

    override fun healthCheckHandler(): Handler<RoutingContext> {
        return Handler { ctx ->
            healthCheckCounter.increment()

            healthCheckTimer.recordCallable {
                circuitBreaker.execute { promise ->
                    logger.debug("received GET request for health check")

                    val response = JsonObject()
                        .put("status", "OK")

                    promise.complete(response)
                }.onComplete { result ->
                    if (result.succeeded()) {
                        logger.debug("health check successful, sending response")
                        healthCheckSuccessCounter.increment()
                        sendResponse(ctx, HttpStatus.OK, result.result())
                    } else {
                        logger.error("health check failed: {}", result.cause().message)
                        healthCheckFailureCounter.increment()
                        sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR,
                            JsonObject().put("message", result.cause().message))
                    }
                }
            }
        }
    }

    // New handler to expose Prometheus metrics
    override fun metricsHandler(): Handler<RoutingContext> {
        return Handler { ctx ->
            logger.debug("received GET request for metrics")

            val prometheusRegistry = meterRegistry as? PrometheusMeterRegistry
            if (prometheusRegistry != null) {
                val metricsText = prometheusRegistry.scrape()
                ctx.response()
                    .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .setStatusCode(HttpStatus.OK)
                    .end(metricsText)
            } else {
                logger.error("Prometheus registry not available")
                ctx.response()
                    .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                    .end("Metrics not available")
            }
        }
    }

    private fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", "service")
        message.put("service", "service")

        logger.trace("Sending response with status code: {}", statusCode)
        ctx.response()
            .setStatusCode(statusCode)
            .end(message.encode())
    }

    // REMOVE THIS METHOD - it conflicts with the property getter
    // fun getMeterRegistry(): MeterRegistry = meterRegistry
}