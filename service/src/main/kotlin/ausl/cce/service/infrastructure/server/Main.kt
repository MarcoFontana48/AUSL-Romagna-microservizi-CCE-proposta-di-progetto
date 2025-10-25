package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.DummyRepository
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.DummyServiceImpl
import ausl.cce.service.infrastructure.persistence.MongoRepository
import io.vertx.core.Vertx
import mf.cce.utils.RepositoryCredentials

/**
 * Main entry point for the service server application.
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
        System.getenv("CONFIG_SERVER_HOST_NAME") ?: "service-mongo-db",
        System.getenv("CONFIG_SERVER_PORT") ?: "27017",
        System.getenv("CONFIG_SERVER_DB_NAME") ?: "service-mongo-db",
        System.getenv("CONFIG_SERVER_DB_USERNAME") ?: "root",
        System.getenv("CONFIG_SERVER_DB_PASSWORD") ?: "password"
    )

    val serviceRepository : DummyRepository = MongoRepository(mongoRepositoryCredentials)
    val service : DummyService = DummyServiceImpl(serviceRepository)

    val serverVerticle = ServerVerticle(service)

    vertx.deployVerticle(serverVerticle).onSuccess {
        println("Service Verticle deployed successfully!")
    }.onFailure { throwable ->
        println("Failed to deploy Service Verticle: ${throwable.message}")
    }
}