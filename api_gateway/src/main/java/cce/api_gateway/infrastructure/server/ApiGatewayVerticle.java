package cce.api_gateway.infrastructure.server;

import cce.api_gateway.application.ApiGatewayController;
import cce.api_gateway.infrastructure.controller.StandardApiGatewayController;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import mf.cce.utils.Endpoints;
import mf.cce.utils.Ports;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The API gateway verticle.
 */
public final class ApiGatewayVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ApiGatewayVerticle.class);
    
    @Override
    public void start() {
        LOGGER.debug("Starting API Gateway");
        
        // === CONFIGURE_CIRCUIT_BREAKER ===
        
        CircuitBreakerOptions options = new CircuitBreakerOptions()
                .setMaxFailures(5)
                .setTimeout(10000)
                .setResetTimeout(30000);
        
        CircuitBreaker circuitBreaker = CircuitBreaker.create("service-circuit-breaker", this.vertx, options);
        
        final ApiGatewayController controller = new StandardApiGatewayController(circuitBreaker);
        
        
        
        // === CONFIGURE_WEB_CLIENT ===
        
        WebClient client = WebClient.create(this.vertx, new WebClientOptions().setDefaultPort(Ports.HTTP));
        
        Router router = Router.router(this.vertx);
        router.route().handler(BodyHandler.create());
        
        
        // === CONFIGURE_ROUTES ===
        
        // reroute to service
        controller.rerouteTo(router, Endpoints.SERVICE + "/*", "service", client);
        
        router.get(Endpoints.HEALTH).handler(controller.healthCheckHandler(client));
        router.get(Endpoints.METRICS).handler(controller.metricsHandler(client));
        
        // start HTTP server
        this.vertx.createHttpServer().requestHandler(router).listen(Ports.HTTP);
        LOGGER.debug("API Gateway ready to serve requests on port {}", Ports.HTTP);
    }
}