package ausl.cce.service.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HealthCheckMetricsProvider
import mf.cce.utils.MetricsProvider

/**
 * Central interface that groups all the service controllers, handlers and metrics providers.
 */
interface ServiceController :
    HealthCheckHandler,
    MetricsHandler,
    HealthCheckMetricsProvider,
    MetricsProvider,
    ClientJsonReplyHandler,
    // === DDD ENTITY HANDLERS ===
    // Dummy (a generic test entity, not to be used in production)
    CRUDDummyHandler,
    DummyMetricsProvider,
    // AllergyIntolerance
    CRUDAllergyIntoleranceHandler,
    AllergyIntoleranceMetricsProvider

/* === CLIENT REPLY HANDLER === */

/**
 * Interface for handling JSON replies to clients.
 */
interface ClientJsonReplyHandler {
    fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject)
    fun sendErrorResponse(ctx: RoutingContext, error: Throwable)
}

/* === HEALTH CHECK HANDLER === */

/**
 * Interface for handling health check requests.
 */
interface HealthCheckHandler {
    fun healthCheckHandler(ctx: RoutingContext)
}

/* === METRICS HANDLER === */

/**
 * Interface for handling metrics requests.
 */
interface MetricsHandler {
    fun metricsHandler(ctx: RoutingContext)
}

/* === DDD ENTITY HANDLERS AND METRICS PROVIDER === */
// Dummy (a generic test entity, not to be used in production)

/**
 * CRUD interface for Dummy entity handlers.
 */
interface CRUDDummyHandler : QueryDummyHandler, CommandDummyHandler

/**
 * Query interface for Dummy entity handlers.
 */
interface QueryDummyHandler {
    fun getDummyHandler(ctx: RoutingContext)
}

/**
 * Command interface for Dummy entity handlers.
 */
interface CommandDummyHandler {
    fun createDummyHandler(ctx: RoutingContext)
    fun updateDummyHandler(ctx: RoutingContext)
    fun deleteDummyHandler(ctx: RoutingContext)
}

/**
 * Metrics provider interface for Dummy entity operations.
 */
interface DummyMetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    /* === READ === */

    val getDummyCounter: Counter
        get() = Counter.builder("get_dummy_requests_total")
            .description("Total number of get_dummy requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getDummySuccessCounter: Counter
        get() = Counter.builder("get_dummy_success_total")
            .description("Total number of successful get_dummy")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getDummyFailureCounter: Counter
        get() = Counter.builder("get_dummy_failure_total")
            .description("Total number of failed get_dummy")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getDummyTimer: Timer
        get() = Timer.builder("get_dummy_duration_seconds")
            .description("get_dummy request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === CREATE === */

    val createDummyCounter: Counter
        get() = Counter.builder("create_dummy_requests_total")
            .description("Total number of create_dummy requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createDummySuccessCounter: Counter
        get() = Counter.builder("create_dummy_success_total")
            .description("Total number of successful create_dummy")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createDummyFailureCounter: Counter
        get() = Counter.builder("create_dummy_failure_total")
            .description("Total number of failed create_dummy")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createDummyTimer: Timer
        get() = Timer.builder("create_dummy_duration_seconds")
            .description("create_dummy request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}

// AllergyIntolerance
/**
 * CRUD interface for AllergyIntolerance entity handlers.
 */
interface CRUDAllergyIntoleranceHandler : QueryAllergyIntoleranceHandler, CommandAllergyIntoleranceHandler

/**
 * Query interface for AllergyIntolerance entity handlers.
 */
interface QueryAllergyIntoleranceHandler {
    fun getAllergyIntoleranceHandler(ctx: RoutingContext)
}

/**
 * Command interface for AllergyIntolerance entity handlers.
 */
interface CommandAllergyIntoleranceHandler {
    fun createAllergyIntoleranceHandler(ctx: RoutingContext)
    fun updateAllergyIntoleranceHandler(ctx: RoutingContext)
    fun deleteAllergyIntoleranceHandler(ctx: RoutingContext)
}

/**
 * Metrics provider interface for AllergyIntolerance entity operations.
 */
interface AllergyIntoleranceMetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    /* === READ === */

    val getAllergyIntoleranceCounter: Counter
        get() = Counter.builder("get_allergy_intolerance_requests_total")
            .description("Total number of get_allergy_intolerance requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getAllergyIntoleranceSuccessCounter: Counter
        get() = Counter.builder("get_allergy_intolerance_success_total")
            .description("Total number of successful get_allergy_intolerance")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getAllergyIntoleranceFailureCounter: Counter
        get() = Counter.builder("get_allergy_intolerance_failure_total")
            .description("Total number of failed get_allergy_intolerance")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getAllergyIntoleranceTimer: Timer
        get() = Timer.builder("get_allergy_intolerance_duration_seconds")
            .description("get_allergy_intolerance request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === CREATE === */

    val createAllergyIntoleranceCounter: Counter
        get() = Counter.builder("create_allergy_intolerance_requests_total")
            .description("Total number of create_allergy_intolerance requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createAllergyIntoleranceSuccessCounter: Counter
        get() = Counter.builder("create_allergy_intolerance_success_total")
            .description("Total number of successful create_allergy_intolerance")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createAllergyIntoleranceFailureCounter: Counter
        get() = Counter.builder("create_allergy_intolerance_failure_total")
            .description("Total number of failed create_allergy_intolerance")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createAllergyIntoleranceTimer: Timer
        get() = Timer.builder("create_allergy_intolerance_duration_seconds")
            .description("create_allergy_intolerance request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}