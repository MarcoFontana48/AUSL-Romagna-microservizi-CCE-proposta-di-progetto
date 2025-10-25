package cce.api_gateway.infrastructure.controller;

import cce.api_gateway.application.ApiGatewayController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import mf.cce.utils.HealthCheckMetricsProvider;
import mf.cce.utils.MetricsProvider;
import mf.cce.utils.HttpStatus;
import mf.cce.utils.Ports;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * StandardApiGatewayController is the standard implementation of the ApiGatewayController interface.
 * It provides functionalities for rerouting requests, handling health checks, and serving metrics.
 */
public class StandardApiGatewayController implements ApiGatewayController, HealthCheckMetricsProvider, MetricsProvider {
    private static final Logger LOGGER = LogManager.getLogger(StandardApiGatewayController.class);
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final String serviceName = "api-gateway";
    
    public StandardApiGatewayController(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    /**
     * Reroutes requests from a specified endpoint to a target host using the provided WebClient.
     *
     * @param router   The Vert.x Router to define routes.
     * @param endpoint The endpoint pattern to reroute (e.g., "/api/*").
     * @param host     The target host to which requests should be rerouted.
     * @param client   The WebClient used to forward requests.
     */
    @Override
    public void rerouteTo(Router router, String endpoint, String host, WebClient client) {
        LOGGER.debug("received request to reroute");
        
        router.route(endpoint).handler(ctx -> this.circuitBreaker.<JsonObject>execute(promise -> {
            String path = ctx.request().uri();
            LOGGER.debug("redirecting request from endpoint: '{}', with uri: '{}' to '{}'", endpoint, path, host);
            String baseEndpoint = endpoint.replace("/*", "");
            if (path.startsWith(baseEndpoint)) {
                path = path.substring(baseEndpoint.length());
            }
            LOGGER.debug("removed endpoint to uri, new uri: '{}' about to be sent to: '{}'", path, host);
            
            String finalPath = path;
            try {
                client.request(ctx.request().method(), Ports.HTTP, host, finalPath).sendBuffer(ctx.getBody(), ar -> {
                    if (ar.succeeded()) {
                        var response = ar.result();
                        ctx.response().setStatusCode(response.statusCode())
                                .putHeader("Content-Type", response.getHeader("Content-Type"));
                        
                        if (response.body() != null) {
                            ctx.response().end(response.body());
                        } else {
                            LOGGER.debug("Response body is null, ending with empty response");
                            ctx.response().end();
                        }
                    } else {
                        ctx.fail(ar.cause());
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
    
    /**
     * Handles health check requests.
     *
     * @param client The WebClient used for any necessary outbound requests.
     * @return A Handler for RoutingContext to process health check requests.
     */
    @Override
    public Handler<RoutingContext> healthCheckHandler(WebClient client) {
        return ctx -> {
            // Use interface methods directly
            this.getHealthCheckCounter().increment();
            this.getMetricsCounter().increment();
            this.getMetricsReadRequestsCounter().increment();
            
            try {
                this.getHealthCheckTimer().recordCallable(() -> {
                    LOGGER.debug("received GET request for health check");
                    
                    this.getHealthCheckSuccessCounter().increment();
                    this.getMetricsSuccessCounter().increment();
                    this.getMetricsSuccessReadRequestsCounter().increment();
                    
                    JsonObject response = new JsonObject().put("status", "OK");
                    sendResponse(ctx, response, HttpStatus.OK);
                    return null;
                });
            } catch (Exception e) {
                this.getHealthCheckFailureCounter().increment();
                this.getMetricsFailureCounter().increment();
                this.getMetricsFailureReadRequestsCounter().increment();
                
                LOGGER.error("Health check failed: {}", e.getMessage());
                JsonObject errorResponse = new JsonObject().put("status", "ERROR").put("message", e.getMessage());
                sendResponse(ctx, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }
    
    /**
     * Handles metrics requests.
     *
     * @param client The WebClient used for any necessary outbound requests.
     * @return A Handler for RoutingContext to process metrics requests.
     */
    @Override
    public Handler<RoutingContext> metricsHandler(WebClient client) {
        return ctx -> {
            // Use metrics interface methods
            this.getMetricsCounter().increment();
            this.getMetricsReadRequestsCounter().increment();
            
            try {
                this.getMetricsTimer().recordCallable(() -> {
                    LOGGER.debug("received GET request for metrics");
                    
                    PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) this.getMeterRegistry();
                    String metricsText = prometheusRegistry.scrape();
                    
                    this.getMetricsSuccessCounter().increment();
                    this.getMetricsSuccessReadRequestsCounter().increment();
                    
                    ctx.response()
                            .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                            .setStatusCode(HttpStatus.OK)
                            .end(metricsText);
                    return null;
                });
            } catch (Exception e) {
                this.getMetricsFailureCounter().increment();
                this.getMetricsFailureReadRequestsCounter().increment();
                
                LOGGER.error("Metrics request failed: {}", e.getMessage());
                ctx.response()
                        .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                        .end("Metrics not available: " + e.getMessage());
            }
        };
    }
    
    /**
     * Sends a JSON response to the client with the specified status code.
     *
     * @param routingContext
     * @param response
     * @param statusCode
     */
    private static void sendResponse(RoutingContext routingContext, JsonObject response, int statusCode) {
        LOGGER.trace("Adding 'service' key to response before sending it to client");
        response.put("service", "api-gateway");
        
        LOGGER.trace("Sending response with status code '{}' to client:\n{}", statusCode, response.encodePrettily());
        routingContext.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(response.encode());
    }
    
    /**
     * Gets the MeterRegistry instance.
     *
     * @return the MeterRegistry
     */
    @Override
    public @NotNull MeterRegistry getMeterRegistry() {
        return this.meterRegistry;
    }
    
    /**
     * Gets the service name.
     *
     * @return the service name
     */
    @Override
    public @NotNull String getServiceName() {
        return this.serviceName;
    }
    
    /**
     * Implementation of HealthCheckMetricsProvider interface
     *
     * @return the health check request counter
     */
    @NotNull
    @Override
    public Counter getHealthCheckCounter() {
        return Counter.builder("health_check_requests_total")
                .description("Total number of health check requests")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of HealthCheckMetricsProvider interface
     *
     * @return the health check success counter
     */
    @NotNull
    @Override
    public Counter getHealthCheckSuccessCounter() {
        return Counter.builder("health_check_success_total")
                .description("Total number of successful health checks")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of HealthCheckMetricsProvider interface
     *
     * @return the health check failure counter
     */
    @NotNull
    @Override
    public Counter getHealthCheckFailureCounter() {
        return Counter.builder("health_check_failure_total")
                .description("Total number of failed health checks")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of HealthCheckMetricsProvider interface
     *
     * @return the health check request timer
     */
    @NotNull
    @Override
    public Timer getHealthCheckTimer() {
        return Timer.builder("health_check_duration_seconds")
                .description("Health check request duration")
                .publishPercentileHistogram()
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of MetricsProvider interface
     *
     * @return the metrics request counter
     */
    @NotNull
    @Override
    public Counter getMetricsCounter() {
        return Counter.builder("metrics_requests_total")
                .description("Total number of metrics requests")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of MetricsProvider interface
     *
     * @return the metrics success counter
     */
    @NotNull
    @Override
    public Counter getMetricsSuccessCounter() {
        return Counter.builder("metrics_success_total")
                .description("Total number of successful metrics")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of MetricsProvider interface
     *
     * @return the metrics failure counter
     */
    @NotNull
    @Override
    public Counter getMetricsFailureCounter() {
        return Counter.builder("metrics_failure_total")
                .description("Total number of failed metrics")
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
    
    /**
     * Implementation of MetricsProvider interface
     *
     * @return the metrics request timer
     */
    @NotNull
    @Override
    public Timer getMetricsTimer() {
        return Timer.builder("metrics_duration_seconds")
                .description("Metrics request duration")
                .publishPercentileHistogram()
                .tag("service", this.getServiceName())
                .register(this.getMeterRegistry());
    }
}