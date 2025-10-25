package cce.api_gateway.application;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import mf.cce.utils.HealthCheckMetricsProvider;
import mf.cce.utils.MetricsProvider;

/**
 * ApiGatewayController defines the contract for an API Gateway controller.
 * It extends HealthCheckMetricsProvider and MetricsProvider to include health check and metrics functionalities.
 */
public interface ApiGatewayController extends HealthCheckMetricsProvider, MetricsProvider {
    void rerouteTo(Router router, String endpoint, String host, WebClient client);
    Handler<RoutingContext> healthCheckHandler(WebClient client);
    Handler<RoutingContext> metricsHandler(WebClient client);
}