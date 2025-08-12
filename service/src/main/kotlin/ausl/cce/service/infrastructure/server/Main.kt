package ausl.cce.service.infrastructure.server

import io.vertx.core.Vertx

fun main() {
    runServer()
}

fun runServer() {
    val vertx: Vertx = Vertx.vertx()

    val serviceVerticle = ServiceVerticle()

    vertx.deployVerticle(serviceVerticle).onSuccess {
        println("Service Verticle deployed successfully!")
    }.onFailure { throwable ->
        println("Failed to deploy Service Verticle: ${throwable.message}")
    }
}