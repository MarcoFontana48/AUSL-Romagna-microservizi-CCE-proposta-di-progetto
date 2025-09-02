package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.AllergyIntoleranceService
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.ServiceController
import ausl.cce.service.infrastructure.controller.StandardController
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
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

class ServerVerticle(
    private val dummyService : DummyService,
    private val allergyIntoleranceService : AllergyIntoleranceService
) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    override fun start() {
        logger.info("Starting server...")

        val meterRegistry = defineMeterRegistry()
        val circuitBreaker = defineCircuitBreaker()
        val controller: ServiceController = StandardController(dummyService, allergyIntoleranceService, circuitBreaker, meterRegistry)
        val router = Router.router(vertx)
        defineEndpoints(router, controller)
        runServer(router).onSuccess {
            logger.info("Server started successfully, listening on port ${Ports.HTTP}")
        }
        .onFailure { throwable ->
            logger.error("Failed to start server: ${throwable.message}")
        }
    }

    private fun defineMeterRegistry(): PrometheusMeterRegistry {
        val config = PrometheusConfig.DEFAULT

        val res = PrometheusMeterRegistry(config)

        Timer.builder("health_check_duration_seconds")
            .description("Health check request duration")
            .tag("service", "anamnesi-pregressa")
            .publishPercentileHistogram() // enables histogram buckets
            .register(res)

        return res
    }

    private fun defineCircuitBreaker(): CircuitBreaker {
        val options = CircuitBreakerOptions()
            .setMaxFailures(5)
            .setTimeout(10000)
            .setResetTimeout(30000)

        return CircuitBreaker.create("anamnesi-pregressa-circuit-breaker", this.vertx, options)
    }

    private fun defineEndpoints(router: Router, controller: ServiceController) {
        router.route().handler(BodyHandler.create())

        /* === HEALTH CHECK ENDPOINT === */
        router.get(Endpoints.HEALTH).handler { ctx -> controller.healthCheckHandler(ctx) }

        /* === METRICS ENDPOINT === */
        router.get(Endpoints.METRICS).handler { ctx -> controller.metricsHandler(ctx) }

        /* === DUMMY DDD ENTITY ENDPOINT === */
        router.get(Endpoints.DUMMIES + "/:id").handler { ctx -> controller.getDummyHandler(ctx) }
        router.post(Endpoints.DUMMIES).handler { ctx -> controller.createDummyHandler(ctx) }

        /* === ALLERGY INTOLERANCE DDD ENTITY ENDPOINT === */
        router.get(Endpoints.ALLERGY_INTOLERANCES + "/:id").handler { ctx -> controller.getAllergyIntoleranceHandler(ctx) }
        router.post(Endpoints.ALLERGY_INTOLERANCES).handler { ctx -> controller.createAllergyIntoleranceHandler(ctx) }
    }

    private fun runServer(router: Router): Future<HttpServer> {
        return this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Ports.HTTP)
    }
}