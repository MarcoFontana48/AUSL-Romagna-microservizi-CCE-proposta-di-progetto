package ausl.cce.service.application

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

interface Controller {
    fun healthCheckHandler(): Handler<RoutingContext>
}