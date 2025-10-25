package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.ServiceController
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import mf.cce.utils.Endpoints
import mf.cce.utils.Ports
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Verticle responsible for setting up and running the HTTP server.
 */
class ServerVerticle(
    private val controller: ServiceController,
) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    override fun start() {
        logger.info("Starting server...")

        val router = Router.router(vertx)
        defineEndpoints(router)
        runServer(router).onSuccess {
            logger.info("Server started successfully, listening on port ${Ports.HTTP}")
        }
            .onFailure { throwable ->
                logger.error("Failed to start server: ${throwable.message}")
            }
    }

    /**
     * Defines the HTTP endpoints and their corresponding handlers.
     */
    private fun defineEndpoints(router: Router) {
        router.route().handler(BodyHandler.create())

        /* === HEALTH CHECK ENDPOINT === */
        router.get(Endpoints.HEALTH).handler { ctx -> controller.healthCheckHandler(ctx) }

        /* === METRICS ENDPOINT === */
        router.get(Endpoints.METRICS).handler { ctx -> controller.metricsHandler(ctx) }

        /* === DUMMY DDD ENTITY ENDPOINT === */
        router.get(Endpoints.DUMMIES + "/:id").handler { ctx -> controller.getDummyHandler(ctx) }
        router.post(Endpoints.DUMMIES).handler { ctx -> controller.createDummyHandler(ctx) }

        /* === CARE PLAN DDD ENTITY ENDPOINTS === */
        router.get(Endpoints.CARE_PLANS + "/:id").handler { ctx -> controller.getCarePlanHandler(ctx) }
        router.post(Endpoints.CARE_PLANS).handler { ctx -> controller.createCarePlanHandler(ctx) }
    }

    /**
     * Configures and runs the HTTP server with specified options.
     */
    private fun runServer(router: Router): Future<HttpServer> {
        val serverOptions = HttpServerOptions()
            .setPort(Ports.HTTP)
            .setHost("0.0.0.0")
            .setTcpKeepAlive(true)
            .setIdleTimeout(300)
            .setAcceptBacklog(2000)
            .setTcpNoDelay(true)
            .setReuseAddress(true)
            .setReusePort(true)
            .setMaxInitialLineLength(8192)
            .setMaxHeaderSize(16384)
            .setCompressionSupported(true)
            .setDecompressionSupported(true)

        return this.vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .listen(Ports.HTTP)
    }
}