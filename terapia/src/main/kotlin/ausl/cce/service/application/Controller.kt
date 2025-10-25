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
    // CarePlan
    CRUDCarePlanHandler,
    CarePlanMetricsProvider

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
 * Interface for Query operations on Dummy entity.
 */
interface QueryDummyHandler {
    fun getDummyHandler(ctx: RoutingContext)
}

/**
 * Interface for Command operations on Dummy entity.
 */
interface CommandDummyHandler {
    fun createDummyHandler(ctx: RoutingContext)
    fun updateDummyHandler(ctx: RoutingContext)
    fun deleteDummyHandler(ctx: RoutingContext)
}

/**
 * Interface for providing metrics related to Dummy entity operations.
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

/**
 * CRUD interface for CarePlan entity handlers.
 */
interface CRUDCarePlanHandler : QueryCarePlanHandler, CommandCarePlanHandler

/**
 * Interface for Query operations on CarePlan entity.
 */
interface QueryCarePlanHandler {
    fun getCarePlanHandler(ctx: RoutingContext)
}

/**
 * Interface for Command operations on CarePlan entity.
 */
interface CommandCarePlanHandler {
    fun createCarePlanHandler(ctx: RoutingContext)
    fun updateCarePlanHandler(ctx: RoutingContext)
    fun deleteCarePlanHandler(ctx: RoutingContext)
}

/**
 * Interface for providing metrics related to CarePlan entity operations.
 */
interface CarePlanMetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    /* === READ === */

    val getCarePlanCounter: Counter
        get() = Counter.builder("get_care_plan_requests_total")
            .description("Total number of get_care_plan requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getCarePlanSuccessCounter: Counter
        get() = Counter.builder("get_care_plan_success_total")
            .description("Total number of successful get_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getCarePlanFailureCounter: Counter
        get() = Counter.builder("get_care_plan_failure_total")
            .description("Total number of failed get_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getCarePlanTimer: Timer
        get() = Timer.builder("get_care_plan_duration_seconds")
            .description("get_care_plan request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === CREATE === */

    val createCarePlanCounter: Counter
        get() = Counter.builder("create_care_plan_requests_total")
            .description("Total number of create_care_plan requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createCarePlanSuccessCounter: Counter
        get() = Counter.builder("create_care_plan_success_total")
            .description("Total number of successful create_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createCarePlanFailureCounter: Counter
        get() = Counter.builder("create_care_plan_failure_total")
            .description("Total number of failed create_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createCarePlanTimer: Timer
        get() = Timer.builder("create_care_plan_duration_seconds")
            .description("create_care_plan request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === UPDATE === */

    val updateCarePlanCounter: Counter
        get() = Counter.builder("update_care_plan_requests_total")
            .description("Total number of update_care_plan requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateCarePlanSuccessCounter: Counter
        get() = Counter.builder("update_care_plan_success_total")
            .description("Total number of successful update_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateCarePlanFailureCounter: Counter
        get() = Counter.builder("update_care_plan_failure_total")
            .description("Total number of failed update_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateCarePlanTimer: Timer
        get() = Timer.builder("update_care_plan_duration_seconds")
            .description("update_care_plan request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === DELETE === */

    val deleteCarePlanCounter: Counter
        get() = Counter.builder("delete_care_plan_requests_total")
            .description("Total number of delete_care_plan requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteCarePlanSuccessCounter: Counter
        get() = Counter.builder("delete_care_plan_success_total")
            .description("Total number of successful delete_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteCarePlanFailureCounter: Counter
        get() = Counter.builder("delete_care_plan_failure_total")
            .description("Total number of failed delete_care_plan")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteCarePlanTimer: Timer
        get() = Timer.builder("delete_care_plan_duration_seconds")
            .description("delete_care_plan request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}