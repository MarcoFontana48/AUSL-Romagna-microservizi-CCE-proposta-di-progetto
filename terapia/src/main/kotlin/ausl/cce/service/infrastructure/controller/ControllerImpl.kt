package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.CarePlanService
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.ServiceController
import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.CarePlanId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import ausl.cce.service.domain.fromJsonToCarePlan
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
    private val dummyService: DummyService,              // 'service' here refers to the DDD service
    private val carePlanService: CarePlanService,              // 'service' here refers to the DDD service
    private val circuitBreaker: CircuitBreaker,
    override val meterRegistry: MeterRegistry,
    override val serviceName: String = "terapia"    // 'service' here is the name of the microservice / server
) : ServiceController {
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

            val dummyEntity = dummyService.getDummyEntityById(DummyId(idParam))
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

    override fun getCarePlanHandler(ctx: RoutingContext) {
        logger.debug("Received GET request for care plan entity")

        getCarePlanCounter.increment()
        metricsCounter.increment()
        metricsReadRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            // expects a request path patameter 'id': "/CarePlan/:id" where ':id' is the care plan entity id, i.e. "/CarePlan/123"
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received GET request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute get request without parameters")
            }
            logger.debug("Extracted care plan entity id from request, 'id': '{}'", idParam)

            val uriAsId = "CarePlan/$idParam"
            logger.debug("Converted care plan entity id to FHIR resource id format: '{}'", uriAsId)

            val carePlanEntity = carePlanService.getCarePlanById(CarePlanId(uriAsId))
            logger.trace("CarePlanEntity retrieved: '{}'", carePlanEntity)

            val replyJson = JsonObject(carePlanEntity.toJson())
            logger.debug("Converted CarePlanEntity to JsonObject: '{}'", replyJson)

            promise.complete(replyJson)
        }.onComplete { result ->
            timerSample.stop(getCarePlanTimer)

            if (result.succeeded()) {
                logger.debug("CarePlanEntity retrieved successfully, sending response")
                getCarePlanSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessReadRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.warn("Failed to retrieve CarePlanEntity, sending response. Error: {}", result.cause().message)
                getCarePlanFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureReadRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun createCarePlanHandler(ctx: RoutingContext) {
        logger.debug("Received POST request to create care plan entity")

        createCarePlanCounter.increment()
        metricsCounter.increment()
        metricsWriteRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            val requestBody = ctx.body().asJsonObject()
            if (requestBody.isEmpty) {
                logger.warn("Received POST request without required body")
                throw IllegalArgumentException("Cannot execute create request without body")
            }
            logger.debug("Extracted care plan entity data from request body: '{}'", requestBody)

            val carePlan = requestBody.toString().fromJsonToCarePlan()
            val entity = CarePlanEntity.of(carePlan)
            logger.debug("Created CarePlanEntity from FHIR resource: '{}'", entity)

            carePlanService.addCarePlan(entity)
            logger.trace("CarePlanEntity created: '{}'", entity)

            promise.complete(JsonObject().put("id", entity.id))
        }.onComplete { result ->
            timerSample.stop(createCarePlanTimer)

            if (result.succeeded()) {
                logger.trace("CarePlanEntity created successfully, sending response")
                createCarePlanSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessWriteRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.CREATED, result.result())
            } else {
                logger.warn("Failed to create CarePlanEntity, sending response. Error: {}", result.cause().message)
                createCarePlanFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureWriteRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun updateCarePlanHandler(ctx: RoutingContext) {
        logger.debug("Received PUT request to update care plan entity")

        updateCarePlanCounter.increment()
        metricsCounter.increment()
        metricsWriteRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received PUT request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute update request without id parameter")
            }

            val requestBody = ctx.body().asJsonObject()
            if (requestBody.isEmpty) {
                logger.warn("Received PUT request without required body")
                throw IllegalArgumentException("Cannot execute update request without body")
            }

            logger.debug("Extracted care plan entity id: '{}' and data from request body: '{}'", idParam, requestBody)

            val uriAsId = "CarePlan/$idParam"
            val carePlan = requestBody.toString().fromJsonToCarePlan()
            val entity = CarePlanEntity.of(CarePlanId(uriAsId), carePlan)
            logger.debug("Created CarePlanEntity from FHIR resource for update: '{}'", entity)

            carePlanService.updateCarePlan(entity)
            logger.trace("CarePlanEntity updated: '{}'", entity)

            promise.complete(JsonObject().put("id", entity.id))
        }.onComplete { result ->
            timerSample.stop(updateCarePlanTimer)

            if (result.succeeded()) {
                logger.trace("CarePlanEntity updated successfully, sending response")
                updateCarePlanSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessWriteRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.warn("Failed to update CarePlanEntity, sending response. Error: {}", result.cause().message)
                updateCarePlanFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureWriteRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun deleteCarePlanHandler(ctx: RoutingContext) {
        logger.debug("Received DELETE request to delete care plan entity")

        deleteCarePlanCounter.increment()
        metricsCounter.increment()
        metricsWriteRequestsCounter.increment()

        val timerSample = Timer.start(meterRegistry)

        circuitBreaker.execute { promise ->
            val idParam = ctx.pathParam("id")
            if (idParam.isNullOrBlank()) {
                logger.warn("Received DELETE request without required 'id' parameter")
                throw IllegalArgumentException("Cannot execute delete request without id parameter")
            }
            logger.debug("Extracted care plan entity id from request, 'id': '{}'", idParam)

            val uriAsId = "CarePlan/$idParam"
            logger.debug("Converted care plan entity id to FHIR resource id format: '{}'", uriAsId)

            carePlanService.deleteCarePlan(CarePlanId(uriAsId))
            logger.trace("CarePlanEntity deleted with id: '{}'", uriAsId)

            promise.complete(JsonObject().put("message", "CarePlan deleted successfully").put("id", uriAsId))
        }.onComplete { result ->
            timerSample.stop(deleteCarePlanTimer)

            if (result.succeeded()) {
                logger.debug("CarePlanEntity deleted successfully, sending response")
                deleteCarePlanSuccessCounter.increment()
                metricsSuccessCounter.increment()
                metricsSuccessWriteRequestsCounter.increment()
                sendResponse(ctx, HttpStatus.OK, result.result())
            } else {
                logger.warn("Failed to delete CarePlanEntity, sending response. Error: {}", result.cause().message)
                deleteCarePlanFailureCounter.increment()
                metricsFailureCounter.increment()
                metricsFailureWriteRequestsCounter.increment()
                sendErrorResponse(ctx, result.cause())
            }
        }
    }

    override fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", "terapia")
        message.put("service", "terapia")

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