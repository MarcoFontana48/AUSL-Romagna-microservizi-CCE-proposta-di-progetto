package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.*
import ausl.cce.service.infrastructure.controller.StandardController
import ausl.cce.service.infrastructure.controller.TerapiaConsumerVerticle
import ausl.cce.service.infrastructure.persistence.MongoCarePlanRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import mf.cce.utils.RepositoryCredentials

fun main() {
    runServer()
}

fun runServer() {
    val vertx: Vertx = Vertx.vertx()

    /* not to be used in production, just for prototyping, not safe */
    val mongoRepositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "terapia-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "terapia-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    val meterRegistry = defineMeterRegistry()
    val circuitBreaker = defineCircuitBreaker(vertx)

    val dummyServiceRepository : DummyRepository = MongoDummyRepository(mongoRepositoryCredentials)
    val dummyService : DummyService = DummyServiceImpl(dummyServiceRepository)

    val carePlanServiceRepository: CarePlanRepository = MongoCarePlanRepository(mongoRepositoryCredentials)
    val terapiaEventProducer = TerapiaProducerVerticle()
    val carePlanService : CarePlanService = CarePlanServiceImpl(carePlanServiceRepository, terapiaEventProducer, meterRegistry, "terapia")

    val controller: ServiceController = StandardController(dummyService, carePlanService, circuitBreaker, meterRegistry)

    val serverVerticle = ServerVerticle(controller)
    val consumerVerticle = TerapiaConsumerVerticle(carePlanService)

    deployVerticles(vertx, serverVerticle, consumerVerticle, terapiaEventProducer)
}

private fun deployVerticles(vertx: Vertx, vararg verticles: Verticle) {
    println("Deploying ${verticles.size} verticles...")
    var counter = 0

    verticles.forEach {
        counter++
        println("Deploying verticle $counter/${verticles.size}: ${it::class.simpleName}...")

        vertx.deployVerticle(it).onSuccess { msg ->
            println("${it::class.simpleName} deployed successfully!")
        }.onFailure { throwable ->
            println("Failed to deploy ${it::class.simpleName}: ${throwable.message}")
        }
    }
}

private fun defineMeterRegistry(): PrometheusMeterRegistry {
    val config = PrometheusConfig.DEFAULT
    val res = PrometheusMeterRegistry(config)

    // Health check timer
    Timer.builder("health_check_duration_seconds")
        .description("Health check request duration")
        .tag("service", "terapia")
        .publishPercentileHistogram() // enables histogram buckets
        .register(res)

    // Any request timer
    Timer.builder("metrics_duration_seconds")
        .description("Any request duration")
        .tag("service", "terapia")
        .publishPercentileHistogram() // enables histogram buckets
        .register(res)

    // Get care plan request timer
    Timer.builder("get_care_plan_duration_seconds")
        .description("care_plan request duration")
        .tag("service", "terapia")
        .publishPercentileHistogram() // enables histogram buckets
        .register(res)

    // Create care plan request timer
    Timer.builder("create_care_plan_duration_seconds")
        .description("care_plan request duration")
        .tag("service", "terapia")
        .publishPercentileHistogram() // enables histogram buckets
        .register(res)

    // Specific request timers can be added here similarly, the timer names should match those used in the ausl.cce.service.application.Controller file

    return res
}

private fun defineCircuitBreaker(vertx: Vertx): CircuitBreaker {
    val options = CircuitBreakerOptions()
        .setMaxFailures(5)
        .setTimeout(10000)
        .setResetTimeout(30000)

    return CircuitBreaker.create("terapia-circuit-breaker", vertx, options)
}