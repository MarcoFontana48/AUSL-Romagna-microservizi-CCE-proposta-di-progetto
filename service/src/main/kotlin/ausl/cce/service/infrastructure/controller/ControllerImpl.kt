package ausl.cce.service.infrastructure.controller

import ausl.cce.service.application.Controller
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mf.cce.utils.HttpStatus
import org.apache.logging.log4j.LogManager

class StandardController(private val circuitBreaker: CircuitBreaker) : Controller {
    private val logger = LogManager.getLogger(this::class.java)

    override fun healthCheckHandler(): Handler<RoutingContext> {
        return Handler { ctx ->
            circuitBreaker.execute { promise ->
                logger.debug("received GET request for health check")

                val response = JsonObject()
                    .put("status", "OK")

                promise.complete(response)
            }.onComplete { result ->
                if (result.succeeded()) {
                    logger.debug("health check successful, sending response")
                    sendResponse(ctx, HttpStatus.OK, result.result())
                } else {
                    logger.error("health check failed: {}", result.cause().message)
                    sendResponse(ctx, HttpStatus.INTERNAL_SERVER_ERROR, JsonObject().put("message", result.cause().message))
                }
            }
        }
    }

    private fun sendResponse(ctx: RoutingContext, statusCode: Int, message: JsonObject) {
        logger.trace("Adding service key to response: '{}'", "service")
        message.put("service", "service")

        logger.trace("Sending response with status code: {}", statusCode)
        ctx.response()
            .setStatusCode(statusCode)
            .end(message.encode())
    }
}