package ausl.cce.endtoend

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import mf.cce.utils.KubernetesTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * Gatling simulation class for health check load testing.
 * This class defines a scenario that simulates sustained load on the health check endpoint.
 */
class HealthCheckLoadTest : Simulation() {
    private val httpProtocol = http
        .baseUrl("http://localhost:31080")
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

/**
 * General performance test class that sets up the environment, runs the Gatling load test,
 * and queries Prometheus for performance metrics.
 */
class GeneralPerformanceTest : KubernetesTest() {
    private val logger = LogManager.getLogger(this::class)
    private val k8sYamlFiles = "src/test/resources/ausl/cce/endtoend"
    private val k8sNamespace = "monitoring-app"
    private lateinit var k8sDirectory: File

    // Service endpoints
    private val apiGatewayUrl = "http://localhost:31080"
    private val prometheusUrl = "http://localhost:31090"
    private val healthEndpoint = "/service/health"

    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    private val serviceName = "service"
    private val healthCheckOperation = "health_check"

    data class TestSummary(
        val scenarioName: String,
        val duration: Duration,
        val p95ResponseTime: Double
    ) {
        private val logger = LogManager.getLogger(this::class)

        fun logSummary() {
            logger.info("=== TEST SUMMARY FOR '$scenarioName' ===")
            logger.info("Test duration: ${duration.toSeconds()} seconds")
            logger.info("95th percentile response time: ${String.format("%.2f", p95ResponseTime)} ms")
            logger.info("==================")
        }
    }

    @AfterEach
    fun tearDown() {
        // Close Vert.x resources
        if (::webClient.isInitialized) {
            webClient.close()
        }
        if (::vertx.isInitialized) {
            vertx.close()
        }

        logger.info("Cleaning up Kubernetes resources...")
        executeKubectlDelete(k8sDirectory)
        logger.info("Kubernetes resources cleaned up")
    }

    @Test
    fun performanceEvaluation() {
        setUpEnvironment(k8sYamlFiles)
        val results = runGatlingLoadTest("Performance evaluation", HealthCheckLoadTest::class.java)
        logger.info("Latency 95 percentile: ${results.p95ResponseTime} ms")

        // OPTIONAL: a constraint can be added here as expected max latency to pass the test (as shown)
        assertTrue(results.p95ResponseTime < 150) // example constraint: p95 should be under 150ms
//        assertTrue(true)
    }

    private fun runGatlingLoadTest(scenarioName: String, simulationClass: Class<out Simulation>): TestSummary {
        logger.info("Starting Gatling load test: $scenarioName")

        val startTime = System.currentTimeMillis()

        // create results directory if it doesn't exist
        val resultsDir = File("target/gatling-results")
        if (!resultsDir.exists()) {
            resultsDir.mkdirs()
        }

        try {
            // run Gatling as separate process to avoid Gradle daemon issues
            val processBuilder = ProcessBuilder().apply {
                command(
                    "java",
                    "-cp", System.getProperty("java.class.path"),
                    "io.gatling.app.Gatling",
                    "-s", simulationClass.name,
                    "-rf", resultsDir.absolutePath,
                    "-bf", "target/classes",
                    "-nr" // no reports
                )
                inheritIO() // this will show Gatling output in console
            }

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            logger.info("Gatling test completed with exit code: $exitCode")

        } catch (e: Exception) {
            logger.error("Error running Gatling test: ${e.message}", e)
        }

        val endTime = System.currentTimeMillis()
        val testDuration = Duration.ofMillis(endTime - startTime)

        // wait for Prometheus to scrape the metrics
        logger.info("Waiting for Prometheus to scrape metrics...")
        Thread.sleep(30000) // 30 seconds

        // Query Prometheus for metrics
        val p95ResponseTime = queryPrometheusForPercentile(healthCheckOperation, serviceName, 0.95)

        return TestSummary(
            scenarioName = scenarioName,
            duration = testDuration,
            p95ResponseTime = p95ResponseTime,
        )
    }

    private fun setUpEnvironment(k8sFilesPath: String) {
        k8sDirectory = File(k8sFilesPath)

        logger.info("Starting Kubernetes resources setup...")
        logger.info("K8s directory path: {}", k8sDirectory.absolutePath)

        // check kubectl is available and cluster is accessible
        checkKubectlAvailability()

        // apply metrics-server first (if not already present)
        try {
            executeKubectlCmd(
                File("."),
                "apply",
                "-f",
                "https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
            )
            logger.info("Metrics server applied")
        } catch (e: Exception) {
            logger.warn("Could not apply metrics server, it may already exist: ${e.message}")
        }

        // apply all YAML files and wait for resources to be ready
        logger.info("Waiting for kubernetes resources to be ready...")
        executeKubectlApplyAndWait(k8sDirectory)

        // wait for deployments and pods to be ready
        waitForDeployments(k8sNamespace, "600s")
        waitForPods(k8sNamespace, "600s")

        vertx = Vertx.vertx()
        webClient = WebClient.create(vertx)

        // wait for services to be fully ready
        Thread.sleep(30000) // 30 seconds

        logger.info("Kubernetes resources are ready for testing")
    }

    private fun queryPrometheusForPercentile(operationType: String, serviceName: String, percentile: Double): Double {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """histogram_quantile($percentile, sum(rate(${operationType}_duration_seconds_bucket{service="$serviceName"}[5m])) by (le))"""

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$prometheusUrl/api/v1/query?query=$encodedQuery&time=$endTime"

            logger.debug("Querying Prometheus for {}th percentile: {}", (percentile * 100).toInt(), prometheusQuery)

            val response = webClient
                .getAbs(prometheusQuery)
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                parsePrometheusValue(body, "${(percentile * 100).toInt()}th percentile")
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                0.0
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus for {}th percentile: {}", (percentile * 100).toInt(), e.message, e)
            0.0
        }
    }

    private fun parsePrometheusValue(body: String, metricName: String): Double {
        logger.debug("Prometheus response for {}: {}", metricName, body)

        // check if result array is empty
        if (body.contains(""""result":[]""")) {
            logger.warn("Prometheus returned empty result set for $metricName")
            return 0.0
        }

        // parse the JSON response to extract the value
        val valueRegex = """"value":\s*\[\s*\d+(?:\.\d+)?,\s*"([^"]+)"\s*]""".toRegex()
        val matchResult = valueRegex.find(body)

        return if (matchResult != null) {
            val valueStr = matchResult.groupValues[1]
            val valueSeconds = valueStr.toDoubleOrNull() ?: 0.0
            val valueMilliseconds = valueSeconds * 1000.0
            logger.trace("Successfully parsed {}: {} seconds ({} ms)", metricName, valueSeconds, valueMilliseconds)
            valueMilliseconds
        } else {
            logger.warn("Could not parse Prometheus response value for {} from body: {}", metricName, body)
            0.0
        }
    }
}