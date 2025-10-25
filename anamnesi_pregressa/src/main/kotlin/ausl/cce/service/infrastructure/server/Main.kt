package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.AllergyIntoleranceService
import ausl.cce.service.application.AllergyIntoleranceServiceImpl
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.DummyServiceImpl
import ausl.cce.service.application.AllergyIntoleranceRepository
import ausl.cce.service.application.AnamnesiProducerVerticle
import ausl.cce.service.application.DummyRepository
import ausl.cce.service.infrastructure.persistence.MongoAllergyIntoleranceRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import mf.cce.utils.RepositoryCredentials

/**
 * Main entry point for the Anamnesi Pregressa Service server application.
 */
fun main() {
    runServer()
}

/**
 * Function to initialize and run the server with necessary verticles and services.
 */
fun runServer() {
    val vertx: Vertx = Vertx.vertx()

    /* not to be used in production, just for prototyping, not safe */
    val mongoRepositoryCredentials = RepositoryCredentials(
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "anamnesi-pregressa-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "anamnesi-pregressa-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    val dummyServiceRepository : DummyRepository = MongoDummyRepository(mongoRepositoryCredentials)
    val dummyService : DummyService = DummyServiceImpl(dummyServiceRepository)

    val allergyIntoleranceServiceRepository: AllergyIntoleranceRepository = MongoAllergyIntoleranceRepository(mongoRepositoryCredentials)
    val anamnesiEventProducer = AnamnesiProducerVerticle()
    val allergyIntoleranceService : AllergyIntoleranceService = AllergyIntoleranceServiceImpl(allergyIntoleranceServiceRepository, anamnesiEventProducer)

    val serverVerticle = ServerVerticle(dummyService, allergyIntoleranceService)

    deployVerticles(vertx, serverVerticle, anamnesiEventProducer)
}

/**
 * Function to deploy multiple verticles on the given Vertx instance.
 */
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