package ausl.cce.service.infrastructure.server

import ausl.cce.service.application.CarePlanService
import ausl.cce.service.application.CarePlanServiceImpl
import ausl.cce.service.application.DummyService
import ausl.cce.service.application.DummyServiceImpl
import ausl.cce.service.infrastructure.controller.TerapiaConsumerVerticle
import ausl.cce.service.infrastructure.controller.TerapiaProducerVerticle
import ausl.cce.service.application.CarePlanRepository
import ausl.cce.service.application.DummyRepository
import ausl.cce.service.infrastructure.persistence.MongoCarePlanRepository
import ausl.cce.service.infrastructure.persistence.MongoDummyRepository
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

    val dummyServiceRepository : DummyRepository = MongoDummyRepository(mongoRepositoryCredentials)
    val dummyService : DummyService = DummyServiceImpl(dummyServiceRepository)

    val carePlanServiceRepository: CarePlanRepository = MongoCarePlanRepository(mongoRepositoryCredentials)
    val terapiaEventProducer = TerapiaProducerVerticle()
    val carePlanService : CarePlanService = CarePlanServiceImpl(carePlanServiceRepository, terapiaEventProducer)

    val serverVerticle = ServerVerticle(dummyService, carePlanService)
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
