package ausl.cce.service.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HealthCheckMetricsProvider
import mf.cce.utils.MetricsProvider

// Updated ServiceController interface with Encounter handlers
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
    // Encounter
    CRUDEncounterHandler,
    EncounterMetricsProvider

/* === CLIENT REPLY HANDLER === */

interface ClientJsonReplyHandler {
    fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject)
    fun sendErrorResponse(ctx: RoutingContext, error: Throwable)
}

/* === HEALTH CHECK HANDLER === */

interface HealthCheckHandler {
    fun healthCheckHandler(ctx: RoutingContext)
}

/* === METRICS HANDLER === */

interface MetricsHandler {
    fun metricsHandler(ctx: RoutingContext)
}

/* === DDD ENTITY HANDLERS AND METRICS PROVIDER === */

// Dummy (a generic test entity, not to be used in production)

interface CRUDDummyHandler : QueryDummyHandler, CommandDummyHandler

interface QueryDummyHandler {
    fun getDummyHandler(ctx: RoutingContext)
}

interface CommandDummyHandler {
    fun createDummyHandler(ctx: RoutingContext)
    fun updateDummyHandler(ctx: RoutingContext)
    fun deleteDummyHandler(ctx: RoutingContext)
}

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

// Encounter
interface CRUDEncounterHandler : QueryEncounterHandler, CommandEncounterHandler

interface QueryEncounterHandler {
    fun getEncounterHandler(ctx: RoutingContext)
}

interface CommandEncounterHandler {
    fun createEncounterHandler(ctx: RoutingContext)
    fun updateEncounterHandler(ctx: RoutingContext)
    fun deleteEncounterHandler(ctx: RoutingContext)
}

interface EncounterMetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    /* === READ === */

    val getEncounterCounter: Counter
        get() = Counter.builder("get_encounter_requests_total")
            .description("Total number of get_encounter requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getEncounterSuccessCounter: Counter
        get() = Counter.builder("get_encounter_success_total")
            .description("Total number of successful get_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getEncounterFailureCounter: Counter
        get() = Counter.builder("get_encounter_failure_total")
            .description("Total number of failed get_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val getEncounterTimer: Timer
        get() = Timer.builder("get_encounter_duration_seconds")
            .description("get_encounter request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === CREATE === */

    val createEncounterCounter: Counter
        get() = Counter.builder("create_encounter_requests_total")
            .description("Total number of create_encounter requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createEncounterSuccessCounter: Counter
        get() = Counter.builder("create_encounter_success_total")
            .description("Total number of successful create_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createEncounterFailureCounter: Counter
        get() = Counter.builder("create_encounter_failure_total")
            .description("Total number of failed create_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val createEncounterTimer: Timer
        get() = Timer.builder("create_encounter_duration_seconds")
            .description("create_encounter request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === UPDATE === */

    val updateEncounterCounter: Counter
        get() = Counter.builder("update_encounter_requests_total")
            .description("Total number of update_encounter requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateEncounterSuccessCounter: Counter
        get() = Counter.builder("update_encounter_success_total")
            .description("Total number of successful update_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateEncounterFailureCounter: Counter
        get() = Counter.builder("update_encounter_failure_total")
            .description("Total number of failed update_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val updateEncounterTimer: Timer
        get() = Timer.builder("update_encounter_duration_seconds")
            .description("update_encounter request duration")
            .tag("service", serviceName)
            .register(meterRegistry)

    /* === DELETE === */

    val deleteEncounterCounter: Counter
        get() = Counter.builder("delete_encounter_requests_total")
            .description("Total number of delete_encounter requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteEncounterSuccessCounter: Counter
        get() = Counter.builder("delete_encounter_success_total")
            .description("Total number of successful delete_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteEncounterFailureCounter: Counter
        get() = Counter.builder("delete_encounter_failure_total")
            .description("Total number of failed delete_encounter")
            .tag("service", serviceName)
            .register(meterRegistry)

    val deleteEncounterTimer: Timer
        get() = Timer.builder("delete_encounter_duration_seconds")
            .description("delete_encounter request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}