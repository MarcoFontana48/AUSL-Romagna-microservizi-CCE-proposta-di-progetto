package cce.api_gateway.infrastructure.server;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    
    public static void main(String[] args) {
        LOGGER.info("Starting server...");
        runServer();
    }
    
    private static void runServer() {
        Vertx vertx = Vertx.vertx();
        
        vertx.deployVerticle(new ApiGatewayVerticle()).onSuccess(id -> LOGGER.info("Server started successfully with deployment ID: {}", id)).onFailure(throwable -> {
            LOGGER.error("Failed to start server: {}", throwable.getMessage());
            vertx.close();
        });
    }
}