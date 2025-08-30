package ausl.cce.endtoend

import io.gatling.javaapi.core.Simulation
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import mf.cce.utils.KubernetesTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

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
        val totalRequestNumber: Int,
        val successfulRequests: Int,
        val p95ResponseTime: Double
    ) {
        private val logger = LogManager.getLogger(this::class)

        fun logSummary() {
            logger.info("=== TEST SUMMARY FOR '$scenarioName' ===")
            logger.info("Test duration: ${duration.toSeconds()} seconds")
            logger.info("Total requests sent: $totalRequestNumber")
            logger.info("Successful requests: $successfulRequests")
            logger.info("95th percentile response time: ${String.format("%.2f", p95ResponseTime)} ms")
            logger.info("==================")
        }
    }

    @BeforeEach
    fun setUp() {
        setUpEnvironment(k8sYamlFiles)
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
    @DisplayName("Test to evaluate architecture performance with sustained average load")
    @Timeout(5 * 60) // 5 minutes timeout
    fun performanceEvaluationSustainedAverageLoad() {
        val results = runLoadTest("Performance evaluation with sustained load", healthCheckOperation, serviceName, HealthCheckLoadTest::class.java)
        results.logSummary()

        // OPTIONAL: a constraint can be added here as expected max latency to pass the test (as shown)
        assertTrue(results.p95ResponseTime < 150) // example constraint: p95 should be under 150ms
//        assertTrue(true)
    }

    @Test
    @DisplayName("Test to evaluate architecture performance with spike average load")
    @Timeout(5 * 60) // 5 minutes timeout
    fun performanceEvaluationSpikeAverageLoad() {
        val results = runLoadTest("Performance evaluation with spike load", healthCheckOperation, serviceName, HealthCheckSpikeTest::class.java)
        results.logSummary()

        // OPTIONAL: a constraint can be added here as expected max latency to pass the test (as shown)
        assertTrue(results.p95ResponseTime < 200) // example constraint: p95 should be under 200ms
//        assertTrue(true)
    }

    @Test
    @DisplayName("Test to evaluate architecture performance with escalating spike load, to identify breaking point")
    @Timeout(15 * 60) // 15 minutes timeout
    fun performanceEvaluationSpikeLoadForBreakingPoint() {
        val results = runLoadTest("Performance evaluation with spike load", healthCheckOperation, serviceName, HealthCheckEscalatingSpikeTest::class.java)
        results.logSummary()

        // to identify breaking point, it's better to visualize latency results over time in the Prometheus GUI using
        // those queries (or going to this url that already has all the queries showed plus others, it is sufficient to
        // refresh the page to see all the new results for each query):
        // http://localhost:31090/query?g0.expr=rate%28health_check_duration_seconds_sum%5B1h%5D%29+%2F+rate%28health_check_duration_seconds_count%5B1h%5D%29&g0.show_tree=0&g0.tab=graph&g0.range_input=1h&g0.res_type=auto&g0.res_density=high&g0.display_mode=stacked&g0.show_exemplars=0&g1.expr=sum%28health_check_success_total%7Bservice%3D%22service%22%7D%29+%2F+sum%28health_check_requests_total%7Bservice%3D%22service%22%7D%29&g1.show_tree=0&g1.tab=graph&g1.range_input=1h&g1.res_type=auto&g1.res_density=medium&g1.display_mode=stacked&g1.show_exemplars=0&g2.expr=sum%28health_check_success_total%7Bservice%3D%22service%22%7D%29&g2.show_tree=0&g2.tab=graph&g2.range_input=1h&g2.res_type=auto&g2.res_density=medium&g2.display_mode=stacked&g2.show_exemplars=0&g3.expr=sum%28health_check_requests_total%7Bservice%3D%22service%22%7D%29&g3.show_tree=0&g3.tab=graph&g3.range_input=1h&g3.res_type=auto&g3.res_density=medium&g3.display_mode=stacked&g3.show_exemplars=0&g4.expr=&g4.show_tree=0&g4.tab=graph&g4.range_input=1h&g4.res_type=auto&g4.res_density=medium&g4.display_mode=lines&g4.show_exemplars=0&g5.expr=&g5.show_tree=0&g5.tab=graph&g5.range_input=1h&g5.res_type=auto&g5.res_density=medium&g5.display_mode=lines&g5.show_exemplars=0&g6.expr=&g6.show_tree=0&g6.tab=graph&g6.range_input=1h&g6.res_type=auto&g6.res_density=medium&g6.display_mode=lines&g6.show_exemplars=0&g7.expr=&g7.show_tree=0&g7.tab=graph&g7.range_input=1h&g7.res_type=auto&g7.res_density=medium&g7.display_mode=lines&g7.show_exemplars=0&g8.expr=&g8.show_tree=0&g8.tab=graph&g8.range_input=1h&g8.res_type=auto&g8.res_density=medium&g8.display_mode=lines&g8.show_exemplars=0&g9.expr=&g9.show_tree=0&g9.tab=graph&g9.range_input=1h&g9.res_type=auto&g9.res_density=medium&g9.display_mode=lines&g9.show_exemplars=0&g10.expr=&g10.show_tree=0&g10.tab=graph&g10.range_input=1h&g10.res_type=auto&g10.res_density=medium&g10.display_mode=lines&g10.show_exemplars=0
        //
        // This shows the average latency over the last hour, showing spikes and trends clearly in the visualized graph:
        //
        // rate(health_check_duration_seconds_sum[1h]) / rate(health_check_duration_seconds_count[1h])
        //
        // It is also recommended to query the amount of successful requests over total to see if any requests failed
        // and its ratio over time:
        //
        // sum(health_check_success_total) / sum(health_check_requests_total)

        assertTrue(true)
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

    private fun runLoadTest(scenarioName: String, operationType: String, serviceName: String, simulationClass: Class<out Simulation>): TestSummary {
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
        val p95ResponseTime = queryForPercentile(prometheusUrl, operationType, serviceName, 0.95)
        val successfulRequests = queryForSuccessfulRequests(prometheusUrl, operationType, serviceName).toInt()
        val totalRequestNumber = queryForTotalRequestsNumber(prometheusUrl, operationType, serviceName).toInt()

        return TestSummary(
            scenarioName = scenarioName,
            duration = testDuration,
            totalRequestNumber = totalRequestNumber,
            successfulRequests = successfulRequests,
            p95ResponseTime = p95ResponseTime,
        )
    }

    // method to perform a custom Prometheus query
    fun executeCustomQuery(metricsServerUrl: String, query: String, queryDescription: String = "custom query"): String? {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$metricsServerUrl/api/v1/query?query=$encodedQuery&time=$endTime"

            logger.debug("Executing custom Prometheus query ({}): {}", queryDescription, prometheusQuery)

            val response = webClient
                .getAbs(prometheusQuery)
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                logger.debug("Custom query ({}) response: {}", queryDescription, body)
                body
            } else {
                logger.warn("Custom Prometheus query ({}) failed with status: {} - {}",
                    queryDescription, response.statusCode(), response.bodyAsString())
                null
            }
        } catch (e: Exception) {
            logger.error("Error executing custom Prometheus query ({}): {}", queryDescription, e.message, e)
            null
        }
    }

    // convenience method to execute custom query and parse single numeric value
    fun executeCustomQueryForValue(metricsServerUrl: String, query: String, queryDescription: String = "custom query"): Double {
        val response = executeCustomQuery(metricsServerUrl, query, queryDescription)

        return if (response != null) {
            parseValue(response, queryDescription)
        } else {
            0.0
        }
    }

    // convenience method to execute custom query and parse counter value
    fun executeCustomQueryForCounterValue(metricsServerUrl: String, query: String, queryDescription: String = "custom query"): Long {
        val response = executeCustomQuery(metricsServerUrl, query, queryDescription)

        return if (response != null) {
            parseCounterValue(response, queryDescription)
        } else {
            0L
        }
    }

    private fun queryForPercentile(metricsServerUrl: String, operationType: String, serviceName: String, percentile: Double): Double {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """histogram_quantile($percentile, sum(rate(${operationType}_duration_seconds_bucket{service="$serviceName"}[1h])) by (le))"""

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$metricsServerUrl/api/v1/query?query=$encodedQuery&time=$endTime"

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
                parseValue(body, "${(percentile * 100).toInt()}th percentile")
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                0.0
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus for {}th percentile: {}", (percentile * 100).toInt(), e.message, e)
            0.0
        }
    }

    private fun queryForSuccessfulRequests(metricsServerUrl: String, operationType: String, serviceName: String): Long {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """sum(${operationType}_success_total{service="$serviceName"})"""

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$metricsServerUrl/api/v1/query?query=$encodedQuery&time=$endTime"

            logger.debug("Querying Prometheus for successful requests: {}", prometheusQuery)

            val response = webClient
                .getAbs(prometheusQuery)
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                parseCounterValue(body, "successful requests")
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                0L
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus for successful requests: {}", e.message, e)
            0L
        }
    }

    private fun queryForTotalRequestsNumber(metricsServerUrl: String, operationType: String, serviceName: String): Long {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """sum(${operationType}_requests_total{service="$serviceName"})"""

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$metricsServerUrl/api/v1/query?query=$encodedQuery&time=$endTime"

            logger.debug("Querying Prometheus for total requests: {}", prometheusQuery)

            val response = webClient
                .getAbs(prometheusQuery)
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                parseCounterValue(body, "total requests")
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                0L
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus for total requests: {}", e.message, e)
            0L
        }
    }

    private fun parseValue(body: String, metricName: String): Double {
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

    private fun parseCounterValue(body: String, metricName: String): Long {
        logger.debug("Prometheus response for {}: {}", metricName, body)

        // check if result array is empty
        if (body.contains(""""result":[]""")) {
            logger.warn("Prometheus returned empty result set for $metricName")
            return 0L
        }

        // parse the JSON response to extract the value
        val valueRegex = """"value":\s*\[\s*\d+(?:\.\d+)?,\s*"([^"]+)"\s*]""".toRegex()
        val matchResult = valueRegex.find(body)

        return if (matchResult != null) {
            val valueStr = matchResult.groupValues[1]
            val value = valueStr.toDoubleOrNull()?.toLong() ?: 0L
            logger.trace("Successfully parsed {}: {}", metricName, value)
            value
        } else {
            logger.warn("Could not parse Prometheus response value for {} from body: {}", metricName, body)
            0L
        }
    }
}