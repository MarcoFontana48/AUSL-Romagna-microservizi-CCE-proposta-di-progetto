package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.DummyService
import ausl.cce.service.application.TerapiaController
import ausl.cce.service.domain.DummyEntity
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
import kotlin.reflect.KClass

class StandardController(
    private val service: DummyService,              // 'service' here refers to the DDD service
    private val circuitBreaker: CircuitBreaker,
    override val meterRegistry: MeterRegistry,
    override val serviceName: String = "terapia"    // 'service' here is the name of the microservice / server
) : TerapiaController {
    private val logger = LogManager.getLogger(this::class.java)
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    private fun <C : Any> deserializeRequestFromClass(jsonRequestBody: JsonObject, klass: KClass<C>): C =
        mapper.readValue(jsonRequestBody.encode(), klass.java)

    override fun healthCheckHandler(ctx: RoutingContext) {
        healthCheckCounter.increment()
        metricsCounter.increment()
        metricsReadRequestsCounter.increment()

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
                metricsSuccessCounter.increment()
                metricsSuccessReadRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.error("health check failed: {}", result.cause().message)
                healthCheckFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureReadRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR,
                    JsonObject().put("message", result.cause().message))
            }
        }
    }

    // handler to expose Prometheus metrics
    override fun metricsHandler(ctx: RoutingContext) {
        metricsCounter.increment()
        metricsReadRequestsCounter.increment()

        metricsTimer.recordCallable {
            logger.debug("received GET request for metrics")

            val prometheusRegistry = meterRegistry as? PrometheusMeterRegistry
            if (prometheusRegistry != null) {
                val metricsText = prometheusRegistry.scrape()
                metricsSuccessCounter.increment()
                metricsSuccessReadRequestsCounter.increment()
                ctx.response()
                    .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .setStatusCode(HttpStatus.OK)
                    .end(metricsText)
            } else {
                logger.error("Prometheus registry not available")
                metricsFailureCounter.increment()
                metricsFailureReadRequestsCounter.increment()
                ctx.response()
                    .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                    .end("Metrics not available")
            }
        }
    }

    override fun getDummyHandler(ctx: RoutingContext) {
        logger.debug("Received GET request for dummy entity")

        getDummyCounter.increment()
        metricsCounter.increment()
        metricsReadRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            // expects a request path patameter 'id': "/dummies/:id" where ':id' is the dummy entity id, i.e. "/dummies/123"
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received GET request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute get request without parameters")
            }
            logger.debug("Extracted dummy entity id from request, 'id': '{}'", idParam)

            val dummyEntity = service.getDummyEntityById(DummyId(idParam))
            logger.trace("DummyEntity retrieved: '{}'", dummyEntity)

            val replyString = mapper.writeValueAsString(dummyEntity)
            val replyJson = JsonObject(replyString)
            logger.debug("Converted DummyEntity to JsonObject: '{}'", replyJson)

            promise.complete(replyJson)
        }.onComplete { result ->
            timerSample.stop(getDummyTimer)

            if (result.succeeded()) {
                logger.debug("DummyEntity retrieved successfully, sending response")
                getDummySuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessReadRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.warn("Failed to retrieve DummyEntity, sending response. Error: {}", result.cause().message)
                getDummyFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureReadRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun createDummyHandler(ctx: RoutingContext) {
        logger.debug("Received POST request to create dummy entity")

        createDummyCounter.increment()
        metricsWriteRequestsCounter.increment()
        metricsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            // expects a request body with the dummy entity data (currently, just an 'id'), i.e.: "/dummies/123"
            val requestBody = ctx.body().asJsonObject()
            if (requestBody.isEmpty) {
                logger.warn("Received POST request without required body")
                throw IllegalArgumentException("Cannot execute create request without body")
            }
            logger.debug("Extracted dummy entity data from request body: '{}'", requestBody)

            logger.trace("About to deserialize request body to DummyEntity class")
            val dummyEntity = deserializeRequestFromClass(requestBody, DummyEntity::class)
            logger.debug("Deserialized DummyEntity from request: '{}'", dummyEntity)

            service.addDummyEntity(dummyEntity)
            logger.trace("DummyEntity created: '{}'", dummyEntity)

            promise.complete(JsonObject().put("id", dummyEntity.id))
        }.onComplete { result ->
            timerSample.stop(createDummyTimer)

            if (result.succeeded()) {
                logger.trace("DummyEntity created successfully, sending response")
                createDummySuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessWriteRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.CREATED, result.result())
            } else {
                logger.warn("Failed to create DummyEntity, sending response. Error: {}", result.cause().message)
                createDummyFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureWriteRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun updateDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    override fun deleteDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    override fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", serviceName)
        message.put("service", serviceName)

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