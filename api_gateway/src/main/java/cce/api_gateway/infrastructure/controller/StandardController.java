package cce.api_gateway.infrastructure.controller;

import cce.api_gateway.application.Controller;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import mf.cce.utils.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardController implements Controller {
    private static final Logger LOGGER = LogManager.getLogger(StandardController.class);
    private static final int HTTP_PORT = 8080;
    private final CircuitBreaker circuitBreaker;
    
    public StandardController(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    @Override
    public void rerouteTo(Router router, String endpoint, String host, WebClient client) {
        LOGGER.debug("received request to reroute");
        
        router.route(endpoint).handler(ctx -> this.circuitBreaker.<JsonObject>execute(promise -> {
            String path = ctx.request().uri();
            LOGGER.debug("redirecting request with uri: '{}' to '{}'", path, host);
            client.request(ctx.request().method(), HTTP_PORT, host, path).sendBuffer(ctx.getBody(), ar -> {
                if (ar.succeeded()) {
                    ctx.response().setStatusCode(ar.result().statusCode())
                            .putHeader("Content-Type", ar.result().getHeader("Content-Type"))
                            .end(ar.result().body());
                } else {
                    ctx.fail(ar.cause());
                }
            });
        }));
    }
    
    @Override
    public Handler<RoutingContext> healthCheckHandler(WebClient client, String host) {
        return ctx -> {
            LOGGER.debug("received GET request for health check");
            
            this.circuitBreaker.<JsonObject>execute(promise -> {
                JsonObject response = new JsonObject().put("status", "OK");
                sendResponse(ctx, response, HttpStatus.OK);
            }).onFailure(failure -> {
                LOGGER.error("Health check failed: {}", failure.getMessage());
                
                JsonObject fallbackResponse = new JsonObject()
                        .put("status", "degraded")
                        .put("message", "Downstream service health check failed")
                        .put("error", failure.getMessage())
                        .put("timestamp", System.currentTimeMillis());
                
                sendResponse(ctx, fallbackResponse, HttpStatus.SERVICE_UNAVAILABLE);
            });
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