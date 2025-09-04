package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.DummyService
import ausl.cce.service.application.DummyServiceImpl
import ausl.cce.service.application.EncounterService
import ausl.cce.service.application.EncounterServiceImpl
import ausl.cce.service.infrastructure.persistence.DummyRepository
import ausl.cce.service.infrastructure.persistence.EncounterRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
import ausl.cce.service.infrastructure.persistence.MongoEncounterRepository
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

    // Add Encounter service setup
    val encounterServiceRepository: EncounterRepository = MongoEncounterRepository(mongoRepositoryCredentials)
    val encounterService: EncounterService = EncounterServiceImpl(encounterServiceRepository)

    // Update ServerVerticle constructor to include encounterService
    val serverVerticle = ServerVerticle(dummyService, encounterService)

    vertx.deployVerticle(serverVerticle).onSuccess {
        println("Service Verticle deployed successfully!")
    }.onFailure { throwable ->
        println("Failed to deploy Service Verticle: ${throwable.message}")
    }
}