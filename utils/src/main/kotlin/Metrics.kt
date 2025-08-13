package mf.cce.utils

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer

/**
 * Health check metrics interface with default implementation
 */
interface HealthCheckMetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    val healthCheckCounter: Counter
        get() = Counter.builder("health_check_requests_total")
            .description("Total number of health check requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val healthCheckSuccessCounter: Counter
        get() = Counter.builder("health_check_success_total")
            .description("Total number of successful health checks")
            .tag("service", serviceName)
            .register(meterRegistry)

    val healthCheckFailureCounter: Counter
        get() = Counter.builder("health_check_failure_total")
            .description("Total number of failed health checks")
            .tag("service", serviceName)
            .register(meterRegistry)

    val healthCheckTimer: Timer
        get() = Timer.builder("health_check_duration_seconds")
            .description("Health check request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}

/**
 * General metrics interface with default implementation
 */
interface MetricsProvider {
    val meterRegistry: MeterRegistry
    val serviceName: String

    val metricsCounter: Counter
        get() = Counter.builder("metrics_requests_total")
            .description("Total number of metrics requests")
            .tag("service", serviceName)
            .register(meterRegistry)

    val metricsSuccessCounter: Counter
        get() = Counter.builder("metrics_success_total")
            .description("Total number of successful metrics")
            .tag("service", serviceName)
            .register(meterRegistry)

    val metricsFailureCounter: Counter
        get() = Counter.builder("metrics_failure_total")
            .description("Total number of failed metrics")
            .tag("service", serviceName)
            .register(meterRegistry)

    val metricsTimer: Timer
        get() = Timer.builder("metrics_duration_seconds")
            .description("Metrics request duration")
            .tag("service", serviceName)
            .register(meterRegistry)
}
