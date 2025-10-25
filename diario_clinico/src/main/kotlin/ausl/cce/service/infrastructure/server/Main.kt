package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.DummyService
import ausl.cce.service.application.DummyServiceImpl
import ausl.cce.service.application.EncounterEventProducerVerticle
import ausl.cce.service.application.EncounterService
import ausl.cce.service.application.EncounterServiceImpl
import ausl.cce.service.application.DummyRepository
import ausl.cce.service.application.EncounterRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
import ausl.cce.service.infrastructure.persistence.MongoEncounterRepository
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
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "diario-clinico-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "diario-clinico-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    val dummyServiceRepository : DummyRepository = MongoDummyRepository(mongoRepositoryCredentials)
    val dummyService : DummyService = DummyServiceImpl(dummyServiceRepository)

    val encounterServiceRepository: EncounterRepository = MongoEncounterRepository(mongoRepositoryCredentials)
    val diarioEventProducer = EncounterEventProducerVerticle()
    val encounterService: EncounterService = EncounterServiceImpl(encounterServiceRepository, diarioEventProducer)

    val serverVerticle = ServerVerticle(dummyService, encounterService)

    deployVerticles(vertx, serverVerticle, diarioEventProducer)
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