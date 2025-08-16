package ausl.cce.service.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HealthCheckMetricsProvider
import mf.cce.utils.MetricsProvider

interface ServiceController :
    HealthCheckHandler,
    MetricsHandler,
    HealthCheckMetricsProvider,
    MetricsProvider,
    CRUDDummyHandler,
    DummyMetricsProvider,
    ClientJsonReplyHandler

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

/* === DUMMY DDD ENTITY HANDLERS === */

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
}
