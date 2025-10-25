package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.AllergyIntoleranceService
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.ServiceController
import ausl.cce.service.domain.AllergyIntoleranceEntity
import ausl.cce.service.domain.AllergyIntoleranceId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import ausl.cce.service.domain.fromJsonToAllergyIntolerance
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

/**
 * Standard implementation of the ServiceController interface to handle requests.
 */
class StandardController(
    private val dummyService: DummyService,              // 'service' here refers to the DDD service
    private val allergyIntoleranceService: AllergyIntoleranceService,              // 'service' here refers to the DDD service
    private val circuitBreaker: CircuitBreaker,
    override val meterRegistry: MeterRegistry,
    override val serviceName: String = "anamnesi-pregressa"    // 'service' here is the name of the microservice / server
) : ServiceController {
    private val logger = LogManager.getLogger(this::class.java)
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    /**
     * Helper function to deserialize a JSON request body into an instance of the specified class.
     *
     * @param jsonRequestBody The JSON request body as a JsonObject.
     * @param klass The KClass of the target type to deserialize into.
     * @return An instance of the specified class populated with data from the JSON request body.
     */
    private fun <C : Any> deserializeRequestFromClass(jsonRequestBody: JsonObject, klass: KClass<C>): C =
        mapper.readValue(jsonRequestBody.encode(), klass.java)

    /**
     * Handler for health check requests.
     */
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
            timerSample.stop(metricsTimer)

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

    /**
     * Handler for metrics requests.
     */
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

    /**
     * Handler for retrieving a Dummy entity by ID.
     */
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

            val dummyEntity = dummyService.getDummyEntityById(DummyId(idParam))
            logger.trace("DummyEntity retrieved: '{}'", dummyEntity)

            val replyString = mapper.writeValueAsString(dummyEntity)
            val replyJson = JsonObject(replyString)
            logger.debug("Converted DummyEntity to JsonObject: '{}'", replyJson)

            promise.complete(replyJson)
        }.onComplete { result ->
            timerSample.stop(getDummyTimer)
            timerSample.stop(metricsTimer)

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

    /**
     * Handler for creating a new Dummy entity.
     */
    override fun createDummyHandler(ctx: RoutingContext) {
        logger.debug("Received POST request to create dummy entity")

        createDummyCounter.increment()
        metricsCounter.increment()
        metricsWriteRequestsCounter.increment()

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

            dummyService.addDummyEntity(dummyEntity)
            logger.trace("DummyEntity created: '{}'", dummyEntity)

            promise.complete(JsonObject().put("id", dummyEntity.id))
        }.onComplete { result ->
            timerSample.stop(createDummyTimer)
            timerSample.stop(metricsTimer)

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

    /**
     * Handler for updating an existing Dummy entity.
     */
    override fun updateDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    /**
     * Handler for deleting a Dummy entity by ID.
     */
    override fun deleteDummyHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    /**
     * Handler for retrieving an AllergyIntolerance entity by ID.
     */
    override fun getAllergyIntoleranceHandler(ctx: RoutingContext) {
        logger.debug("Received GET request for allergy intolerance entity")

        getAllergyIntoleranceCounter.increment()
        metricsCounter.increment()
        metricsReadRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            // expects a request path patameter 'id': "/AllergyIntolerance/:id" where ':id' is the allergy intolerance entity id, i.e. "/AllergyIntolerance/123"
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received GET request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute get request without parameters")
            }
            logger.debug("Extracted allergy intolerance entity id from request, 'id': '{}'", idParam)

            val uriAsId = "AllergyIntolerance/$idParam"
            logger.debug("Converted allergy intolerance entity id to FHIR resource id format: '{}'", uriAsId)

            val allergyIntoleranceEntity = allergyIntoleranceService.getAllergyIntoleranceById(AllergyIntoleranceId(uriAsId))
            logger.trace("AllergyIntoleranceEntity retrieved: '{}'", allergyIntoleranceEntity)

            val replyJson = JsonObject(allergyIntoleranceEntity.toJson())
            logger.debug("Converted AllergyIntoleranceEntity to JsonObject: '{}'", replyJson)

            promise.complete(replyJson)
        }.onComplete { result ->
            timerSample.stop(getAllergyIntoleranceTimer)
            timerSample.stop(metricsTimer)

            if (result.succeeded()) {
                logger.debug("AllergyIntoleranceEntity retrieved successfully, sending response")
                getAllergyIntoleranceSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessReadRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.warn("Failed to retrieve AllergyIntoleranceEntity, sending response. Error: {}", result.cause().message)
                getAllergyIntoleranceFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureReadRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    /**
     * Handler for creating a new AllergyIntolerance entity.
     */
    override fun createAllergyIntoleranceHandler(ctx: RoutingContext) {
        logger.debug("Received POST request to create allergy intolerance entity")

        createAllergyIntoleranceCounter.increment()
        metricsCounter.increment()
        metricsWriteRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            val requestBody = ctx.body().asJsonObject()
            if (requestBody.isEmpty) {
                logger.warn("Received POST request without required body")
                throw IllegalArgumentException("Cannot execute create request without body")
            }
            logger.debug("Extracted allergy intolerance entity data from request body: '{}'", requestBody)

            val allergyIntolerance = requestBody.toString().fromJsonToAllergyIntolerance()
            val entity = AllergyIntoleranceEntity.of(allergyIntolerance)
            logger.debug("Created AllergyIntoleranceEntity from FHIR resource: '{}'", entity)

            allergyIntoleranceService.addAllergyIntolerance(entity)
            logger.trace("AllergyIntoleranceEntity created: '{}'", entity)

            promise.complete(JsonObject().put("id", entity.id))
        }.onComplete { result ->
            timerSample.stop(createAllergyIntoleranceTimer)
            timerSample.stop(metricsTimer)

            if (result.succeeded()) {
                logger.trace("AllergyIntoleranceEntity created successfully, sending response")
                createAllergyIntoleranceSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessWriteRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.CREATED, result.result())
            } else {
                logger.warn("Failed to create AllergyIntoleranceEntity, sending response. Error: {}", result.cause().message)
                createAllergyIntoleranceFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureWriteRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    /**
     * Handler for updating an existing AllergyIntolerance entity.
     */
    override fun updateAllergyIntoleranceHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    /**
     * Handler for deleting an AllergyIntolerance entity by ID.
     */
    override fun deleteAllergyIntoleranceHandler(ctx: RoutingContext) {
        TODO("Not yet implemented")
    }

    /**
     * Sends a JSON response with the specified status code.
     *
     * @param ctx The routing context.
     * @param statusCode The HTTP status code to send.
     * @param message The JSON message to include in the response.
     */
    override fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", "anamnesi-pregressa")
        message.put("service", "anamnesi-pregressa")

        logger.trace("Sending response with status code: {}", statusCode)
        ctx.response()
            .setStatusCode(statusCode)
            .end(message.encode())
    }

    /**
     * Sends an error response based on the type of the provided Throwable.
     *
     * @param ctx The routing context.
     * @param error The Throwable representing the error.
     */
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