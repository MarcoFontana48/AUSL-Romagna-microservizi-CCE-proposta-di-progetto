package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.ServiceController
import ausl.cce.service.infrastructure.controller.StandardController
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import mf.cce.utils.Endpoints
import mf.cce.utils.Ports
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class ServiceVerticle : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    override fun start() {
        logger.info("Starting service...")

        val circuitBreaker = defineCircuiteBreaker()
        val controller: ServiceController = StandardController(circuitBreaker)
        val router = Router.router(vertx)
        defineEndpoints(router, controller)
        runServer(router).onSuccess {
            logger.info("Service started successfully, listening on port ${Ports.HTTP}")
        }
        .onFailure { throwable ->
            logger.error("Failed to start service: ${throwable.message}")
        }
    }

    private fun defineCircuiteBreaker(): CircuitBreaker {
        val options = CircuitBreakerOptions()
            .setMaxFailures(5)
            .setTimeout(10000)
            .setResetTimeout(30000)

        return CircuitBreaker.create("service-circuit-breaker", this.vertx, options)
    }

    private fun runServer(router: Router): Future<HttpServer> {
        return this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Ports.HTTP)
    }

    private fun defineEndpoints(router: Router, controller: ServiceController) {
        router.route().handler(BodyHandler.create())

        router.get(Endpoints.HEALTH).handler(controller.healthCheckHandler())
        router.get(Endpoints.METRICS).handler(controller.metricsHandler())
    }
}