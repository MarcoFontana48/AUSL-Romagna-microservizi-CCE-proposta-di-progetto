package ausl.cce.endtoend

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.OpenInjectionStep.nothingFor
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration

/*
    === PERFORMANCE MEASURING TESTS ===

    The following tests are designed to measure the performance of the system under different load conditions.
    They include:
    - Sustained Load Test: simulates a steady load over time.
    - Spike Load Test: simulates a sudden spike in load.
    - Escalating Spike Test: simulates escalating spikes every 30 seconds, starting from K request and increasing by M
      requests each time up to N requests, to evaluate the limits of the architecture identifying the breaking point.
*/

/**
 * Gatling simulation class for health check load testing.
 * This class defines a scenario that simulates escalating spikes every 30 seconds,
 * starting from 1 request and increasing by 1 each time up to 5 requests, to evaluate the limits of the
 * architecture identifying the breaking point.
 *
 * So the spikes are:
 * - Spike 1: sending 1 request at t=0s
 * - Spike 2: sending 2 requests at t=30s
 * - ...
 * - Spike 5: sending 5 requests at t=120s
 *
 * Keep in mind that numbers are only indicative and can be adjusted based on the expected load and system capacity.
 * For this test, I've chosen a low number of requests to avoid overwhelming the system I'm using for the test, but in
 * a real case scenario much higher amounts should be used instead, to identify the real breaking point.
 */
class HealthCheckEscalatingSpikeTest : Simulation() {
    private val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling Load Test")

    // Create individual scenarios for each spike level
    private val spike1Scenario = scenario("Spike 1 Requests")
        .repeat(1).on(
            exec(
                http("health_check_spike_1")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    private val spike2Scenario = scenario("Spike 2 Requests")
        .repeat(2).on(
            exec(
                http("health_check_spike_2")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    private val spike3Scenario = scenario("Spike 3 Requests")
        .repeat(3).on(
            exec(
                http("health_check_spike_3")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    private val spike4Scenario = scenario("Spike 4 Requests")
        .repeat(4).on(
            exec(
                http("health_check_spike_4")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    private val spike5Scenario = scenario("Spike 5 Requests")
        .repeat(5).on(
            exec(
                http("health_check_spike_5")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    init {
        setUp(
            // Spike 1: 1 requests at t=0
            spike1Scenario.injectOpen(
                nothingFor(Duration.ofSeconds(0)),
                atOnceUsers(1)
            ),
            // Spike 2: 2 requests at t=30s
            spike2Scenario.injectOpen(
                nothingFor(Duration.ofSeconds(30)),
                atOnceUsers(1)
            ),
            // Spike 3: 3 requests at t=60s
            spike3Scenario.injectOpen(
                nothingFor(Duration.ofSeconds(60)),
                atOnceUsers(1)
            ),
            // Spike 4: 4 requests at t=90s
            spike4Scenario.injectOpen(
                nothingFor(Duration.ofSeconds(90)),
                atOnceUsers(1)
            ),
            // Spike 5: 5 requests at t=120s
            spike5Scenario.injectOpen(
                nothingFor(Duration.ofSeconds(120)),
                atOnceUsers(1)
            ),
        ).protocols(httpProtocol)
            .maxDuration(Duration.ofSeconds(300)) // allow time for all spikes to complete
    }
}

/**
 * Gatling simulation class for health check load testing.
 * This class defines a scenario that simulates a sudden spike on load on the health check endpoint.
 */
class HealthCheckSpikeTest : Simulation() {
    private val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling Load Test")

    // scenario for spike load, where 100 requests are sent all at once
    private val spikeLoadScenario = scenario("Spike Load Test")
        // send 100 requests immediately
        .repeat(100).on(
            exec(
                http("health_check_spike")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
        )

    init {
        setUp(
            spikeLoadScenario.injectOpen(
                atOnceUsers(1)
            )
        ).protocols(httpProtocol)
            .maxDuration(Duration.ofSeconds(30))
    }
}

/**
 * Gatling simulation class for health check load testing.
 * This class defines a scenario that simulates sustained load over time on the health check endpoint.
 */
class HealthCheckLoadTest : Simulation() {
    private val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling Load Test")

    // scenario for sustained load, where each user sends 10 requests, 1 request per second
    private val sustainedLoadScenario = scenario("Sustained Load Test")
        // send 10 requests per user with 1 second pause
        .repeat(10).on( // each user will repeat this X times
            exec(
                http("health_check_sustained")
                    .get("/service/health")
                    .check(status().`in`(200, 201, 202))
            )
                .pause(Duration.ofSeconds(1))
        )

    init {
        // create 4 users, each will run for the amount of iterations with 1 sec pause specified above
        setUp(
            sustainedLoadScenario.injectOpen(
                atOnceUsers(4) // X users start immediately
            )
        ).protocols(httpProtocol)
            .maxDuration(Duration.ofSeconds(40))
    }
}


