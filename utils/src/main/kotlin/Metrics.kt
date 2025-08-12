package mf.cce.utils

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlin.getValue

abstract class AbstractRegistryController(
    private val serviceName: String
) {
    protected abstract val meterRegistry: MeterRegistry

    protected val healthCheckCounter by lazy {
        Counter.builder("health_check_requests_total")
            .description("Total number of health check requests")
            .tag("service", serviceName)
            .register(meterRegistry)
    }

    protected val healthCheckSuccessCounter by lazy {
        Counter.builder("health_check_success_total")
            .description("Total number of successful health checks")
            .tag("service", serviceName)
            .register(meterRegistry)
    }

    protected val healthCheckFailureCounter by lazy {
        Counter.builder("health_check_failure_total")
            .description("Total number of failed health checks")
            .tag("service", serviceName)
            .register(meterRegistry)
    }

    protected val healthCheckTimer by lazy {
        Timer.builder("health_check_duration_seconds")
            .description("Health check request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
    }
}