package ausl.cce.service.application

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HealthCheckMetricsProvider
import mf.cce.utils.MetricsProvider

interface ServiceController : HealthCheckHandler, MetricsHandler, HealthCheckMetricsProvider, MetricsProvider

interface HealthCheckHandler {
    fun healthCheckHandler(): Handler<RoutingContext>
}

interface MetricsHandler {
    fun metricsHandler(): Handler<RoutingContext>
}