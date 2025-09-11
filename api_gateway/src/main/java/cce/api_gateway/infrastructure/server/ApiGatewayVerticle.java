package cce.api_gateway.infrastructure.server;

import cce.api_gateway.application.ApiGatewayController;
import cce.api_gateway.infrastructure.controller.StandardApiGatewayController;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
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
        
        WebClientOptions webClientOptions = new WebClientOptions()
                .setDefaultPort(Ports.HTTP)
                .setMaxPoolSize(100)
                .setMaxWaitQueueSize(50)
                .setKeepAlive(true)
                .setKeepAliveTimeout(30)
                .setPipelining(false)
                .setConnectTimeout(5000)
                .setIdleTimeout(60)
                .setTryUseCompression(true)
                .setTcpKeepAlive(true)
                .setTcpNoDelay(true);
        
        WebClient client = WebClient.create(this.vertx, webClientOptions);
        Router router = Router.router(this.vertx);
        router.route().handler(BodyHandler.create());
        
        
        // === CONFIGURE_ROUTES ===
        
        // reroute to service
        controller.rerouteTo(router, Endpoints.SERVICE + "/*", "service", client);
        controller.rerouteTo(router, Endpoints.TERAPIA + "/*", "terapia", client);
        controller.rerouteTo(router, Endpoints.DIARIO_CLINICO + "/*", "diario-clinico", client);
        controller.rerouteTo(router, Endpoints.ANAMNESI_PREGRESSA + "/*", "anamnesi-pregressa", client);
        
        router.get(Endpoints.HEALTH).handler(controller.healthCheckHandler(client));
        router.get(Endpoints.METRICS).handler(controller.metricsHandler(client));
        
        // start HTTP server
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setPort(Ports.HTTP)
                .setHost("0.0.0.0")
                .setTcpKeepAlive(true)
                .setIdleTimeout(120)
                .setAcceptBacklog(1000)
                .setTcpNoDelay(true)
                .setReuseAddress(true)
                .setReusePort(true);
        
        this.vertx.createHttpServer(serverOptions)
                .requestHandler(router)
                .listen(Ports.HTTP);
        
        LOGGER.debug("API Gateway ready to serve requests on port {}", Ports.HTTP);
    }
}