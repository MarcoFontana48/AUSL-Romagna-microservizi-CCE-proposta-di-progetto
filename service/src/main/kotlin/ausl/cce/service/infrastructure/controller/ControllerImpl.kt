package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.DummyService
import ausl.cce.service.application.ServiceController
import ausl.cce.service.domain.DummyEntity.DummyId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HttpStatus
import org.apache.logging.log4j.LogManager
import java.sql.SQLIntegrityConstraintViolationException

class StandardController(
    private val service: DummyService,              // 'service' here refers to the DDD service
    private val circuitBreaker: CircuitBreaker,
    override val meterRegistry: MeterRegistry,
    override val serviceName: String = "service"    // 'service' here is the name of the microservice / server
) : ServiceController {
    private val logger = LogManager.getLogger(this::class.java)
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    override fun healthCheckHandler(ctx: RoutingContext) {
        healthCheckCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            logger.debug("received GET request for health check")

            val response = JsonObject()
                .put("status", "OK")

            promise.complete(response)
        }.onComplete { result ->
            timerSample.stop(healthCheckTimer) // Stop timer when async work completes

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

    // handler to expose Prometheus metrics
    override fun metricsHandler(ctx: RoutingContext) {
        metricsCounter.increment()

        metricsTimer.recordCallable {
            logger.debug("received GET request for metrics")

            val prometheusRegistry = meterRegistry as? PrometheusMeterRegistry
            if (prometheusRegistry != null) {
                val metricsText = prometheusRegistry.scrape()
                metricsSuccessCounter.increment()
                ctx.response()
                    .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .setStatusCode(HttpStatus.OK)
                    .end(metricsText)
            } else {
                logger.error("Prometheus registry not available")
                metricsFailureCounter.increment()
                ctx.response()
                    .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                    .end("Metrics not available")
            }
        }
    }

    override fun getDummyHandler(ctx: RoutingContext) {
        logger.debug("Received GET request for dummy entity")

        getDummyCounter.increment()
        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute<JsonObject> { promise ->
            // expects a request path patameter 'id': "/dummies/:id" where ':id' is the dummy entity id, i.e. "/dummies/123"
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received GET request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute get request without parameters")
            }
            logger.debug("Extracted dummy entity id from request, 'id': '{}'", idParam)

            val dummyEntity = service.getDummyEntityById(DummyId(idParam))
            logger.trace("DummyEntity retrieved: '{}'", dummyEntity)

            mapper.writeValueAsString(dummyEntity)
        }.onComplete { result ->
            timerSample.stop(getDummyTimer)

            if (result.succeeded()) {
                getDummySuccessCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                getDummyFailureCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun createDummyHandler(ctx: RoutingContext) {
        logger.debug("Received POST request to create dummy entity")


    }

    override fun updateDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    override fun deleteDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    override fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", "service")
        message.put("service", "service")

        logger.trace("Sending response with status code: {}", statusCode)
        ctx.response()
            .setStatusCode(statusCode)
            .end(message.encode())
    }

    override fun sendErrorResponse(ctx: RoutingContext, error: Throwable) {
        logger.trace("Converting error message: '{}' to JsonObject", error.message)

        val errorJson = JsonObject()
            .put("error", error.message ?: "Unknown error")
            .put("type", error.javaClass.simpleName)

        logger.trace("About to send error response with json error: '{}'", errorJson)
        when (error) {
            is IllegalArgumentException, is MismatchedInputException -> { sendResponse(ctx, HttpStatus.BAD_REQUEST, errorJson) }
            is IllegalStateException -> { sendResponse(ctx, HttpStatus.NOT_FOUND, errorJson) }
            is UnsupportedOperationException -> { sendResponse(ctx, HttpStatus.FORBIDDEN, errorJson) }
            is SQLIntegrityConstraintViolationException -> { sendResponse(ctx, HttpStatus.FORBIDDEN, errorJson) }
            else -> { sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, errorJson) }
        }
    }
}