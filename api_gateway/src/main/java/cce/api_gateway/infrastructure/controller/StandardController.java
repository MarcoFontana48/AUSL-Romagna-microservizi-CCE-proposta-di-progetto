package cce.api_gateway.infrastructure.controller;

import cce.api_gateway.application.Controller;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import mf.cce.utils.AbstractRegistryController;
import mf.cce.utils.HttpStatus;
import mf.cce.utils.Ports;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class StandardController extends AbstractRegistryController implements Controller {
    private static final Logger LOGGER = LogManager.getLogger(StandardController.class);
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;
    
    public StandardController(CircuitBreaker circuitBreaker) {
        super("api-gateway");
        this.circuitBreaker = circuitBreaker;
        this.meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    // Implementation of kotlin's abstract property as getter method
    @NotNull
    @Override
    protected MeterRegistry getMeterRegistry() {
        return this.meterRegistry;
    }
    
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
    
    @Override
    public Handler<RoutingContext> healthCheckHandler(WebClient client) {
        return ctx -> {
            this.getHealthCheckCounter().increment();
            
            try {
                this.getHealthCheckTimer().recordCallable(() -> {
                    LOGGER.debug("received GET request for health check");
                    
                    this.getHealthCheckSuccessCounter().increment();
                    JsonObject response = new JsonObject().put("status", "OK");
                    sendResponse(ctx, response, HttpStatus.OK);
                    return null;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    @Override
    public Handler<RoutingContext> metricsHandler(WebClient client) {
        return ctx -> {
            LOGGER.debug("received GET request for metrics");
            
            PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) this.getMeterRegistry();
            String metricsText = prometheusRegistry.scrape();
            ctx.response()
                    .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .setStatusCode(HttpStatus.OK)
                    .end(metricsText);
        };
    }
    
    private static void sendResponse(RoutingContext routingContext, JsonObject response, int statusCode) {
        LOGGER.trace("Adding 'service' key to response before sending it to client");
        response.put("service", "api-gateway");
        
        LOGGER.trace("Sending response with status code '{}' to client:\n{}", statusCode, response.encodePrettily());
        routingContext.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(response.encode());
    }
}