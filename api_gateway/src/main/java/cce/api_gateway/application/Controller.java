package cce.api_gateway.application;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

public interface Controller {
    void rerouteTo(Router router, String endpoint, String host, WebClient client);
    Handler<RoutingContext> healthCheckHandler(WebClient client);
}