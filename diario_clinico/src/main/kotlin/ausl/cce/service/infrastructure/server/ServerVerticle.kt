package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.DummyService
import ausl.cce.service.application.EncounterService
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
    private val encounterService : EncounterService
) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    override fun start() {
        logger.info("Starting server...")

        val meterRegistry = defineMeterRegistry()
        val circuitBreaker = defineCircuitBreaker()
        val controller: ServiceController = StandardController(
            dummyService,
            encounterService,
            circuitBreaker,
            meterRegistry
        )
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
            .tag("service", "diario-clinico")
            .publishPercentileHistogram() // enables histogram buckets
            .register(res)

        // Any request timer
        Timer.builder("metrics_duration_seconds")
            .description("Any request duration")
            .tag("service", "diario-clinico")
            .publishPercentileHistogram() // enables histogram buckets
            .register(res)

        // Get allergy request timer
        Timer.builder("get_encounter_duration_seconds")
            .description("encounter request duration")
            .tag("service", "diario-clinico")
            .publishPercentileHistogram() // enables histogram buckets
            .register(res)

        // Create allergy request timer
        Timer.builder("create_encounter_duration_seconds")
            .description("encounter request duration")
            .tag("service", "diario-clinico")
            .publishPercentileHistogram() // enables histogram buckets
            .register(res)

        return res
    }

    private fun defineCircuitBreaker(): CircuitBreaker {
        val options = CircuitBreakerOptions()
            .setMaxFailures(5)
            .setTimeout(10000)
            .setResetTimeout(30000)

        return CircuitBreaker.create("diario-clinico-circuit-breaker", this.vertx, options)
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

        /* === ENCOUNTER DDD ENTITY ENDPOINT === */
        router.get(Endpoints.ENCOUNTERS + "/:id").handler { ctx -> controller.getEncounterHandler(ctx) }
        router.post(Endpoints.ENCOUNTERS).handler { ctx -> controller.createEncounterHandler(ctx) }
    }

    private fun runServer(router: Router): Future<HttpServer> {
        return this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Ports.HTTP)
    }
}