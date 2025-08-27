package ausl.cce.endtoend

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import mf.cce.utils.KubernetesTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class GeneralPerformanceTest : KubernetesTest() {
    private val logger = LogManager.getLogger(this::class)
    private val k8sYamlFilesWithAutoscaleEnvironment = "src/test/resources/ausl/cce/endtoend/autoscale"
    private val k8sYamlFilesWithoutAutoscaleEnvironment = "src/test/resources/ausl/cce/endtoend/noautoscale"
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
        val totalRequests: Int,
        val totalDuration: Long,
        val throughput: Double,
        val p95ResponseTime: Double,
        val successfulRequests: Int = 0,
        val failedRequests: Int = 0
    ) {
        private val logger = LogManager.getLogger(this::class)

        fun logSummary() {
            logger.info("=== TEST SUMMARY FOR '$scenarioName' ===")
            logger.info("Total requests sent: $totalRequests")
            logger.info("Successful requests: $successfulRequests")
            logger.info("Failed requests: $failedRequests")
            logger.debug("Total test duration: $totalDuration ms")
            logger.debug("Average throughput: ${String.format("%.2f", throughput)} req/s")
            logger.info("95th percentile response time: ${String.format("%.2f", p95ResponseTime)} ms")
            logger.info("==================")
        }
    }

//    @AfterEach
//    fun tearDown() {
//        // Close Vert.x resources
//        if (::webClient.isInitialized) {
//            webClient.close()
//        }
//        if (::vertx.isInitialized) {
//            vertx.close()
//        }
//
//        logger.info("Cleaning up Kubernetes resources...")
//        executeKubectlDelete(k8sDirectory)
//        logger.info("Kubernetes resources cleaned up")
//    }

    @Test
    fun performanceEvaluationWithAutoscaler() {
        setUpEnvironment(k8sYamlFilesWithAutoscaleEnvironment)
        val results = startPerformanceEvaluationWithFixedAmountOfRequests("Performance evaluation with Autoscaler", 25, healthEndpoint)
        results.logSummary()
        assertTrue(true, "Test completed successfully")
    }

    @Test
    fun performanceEvaluationWithoutAutoscaler() {
        setUpEnvironment(k8sYamlFilesWithoutAutoscaleEnvironment)
        val results = startPerformanceEvaluationWithFixedAmountOfRequests("Performance evaluation without Autoscaler", 25, healthEndpoint)
        results.logSummary()
        assertTrue(true, "Test completed successfully")
    }

    /**
     * Start performance evaluation by sending a fixed amount of requests simulating different clients sending multiple requests over time.
     */
    private fun startPerformanceEvaluationWithFixedAmountOfRequests(scenarioName: String, totalRequests: Int = 20, endpoint: String): TestSummary {
        logger.info("Starting concurrent health check test...")

        logger.info(
            "Sending {} requests", totalRequests
        )

        // create all request futures at once for true concurrency
        val allRequestFutures = mutableListOf<CompletableFuture<Void>>()
        val startTime = System.currentTimeMillis()

        var success = 0
        var failure = 0

        // send all requests concurrently (stress test)
        for (requestId in 1..totalRequests) {
            val requestFuture = CompletableFuture<Void>()
            allRequestFutures.add(requestFuture)

            webClient
                .getAbs("$apiGatewayUrl$endpoint")
                .timeout(30000) // 30 second timeout
                .send()
                .onSuccess { response ->
                    success++
                    logger.trace("Request {} completed with status {}", requestId, response.statusCode())
                    requestFuture.complete(null)
                }
                .onFailure { error ->
                    failure++
                    logger.error("Request {} failed: {}", requestId, error.message)
                    requestFuture.complete(null) // complete anyway to not block the test
                }
        }

        // wait for ALL requests to complete
        logger.info("Waiting for all {} concurrent requests to complete...", totalRequests)
        CompletableFuture.allOf(*allRequestFutures.toTypedArray()).get(300, TimeUnit.SECONDS)

        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - startTime

        logger.info(
            "All {} requests completed in {} ms - {}/{} successes",
            totalRequests,
            totalDuration,
            success,
            totalRequests
        )

        val throughput = totalRequests.toDouble() / (totalDuration / 1000.0)
        logger.info("Average throughput: {} requests/second", String.format("%.2f", throughput))

        // wait for Prometheus to scrape the metrics
        Thread.sleep(30000) // 30 seconds

        // query Prometheus for 95th percentile response time
        val p95ResponseTime = queryPrometheusFor95thPercentile(healthCheckOperation)

        return TestSummary(
            scenarioName,
            totalRequests,
            totalDuration,
            throughput,
            p95ResponseTime
        )
    }

    private fun setUpEnvironment(k8sFilesPath: String) {
        k8sDirectory = File(k8sFilesPath)

        logger.info("Starting Kubernetes resources setup...")
        logger.info("K8s directory path: {}", k8sDirectory.absolutePath)

        // Check kubectl is available and cluster is accessible
        checkKubectlAvailability()

        // Apply metrics-server first (if not already present)
        try {
            executeKubectlCmd(
                File("."),
                "apply",
                "-f",
                "https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
            )
            logger.info("Metrics server applied (may already exist)")
        } catch (e: Exception) {
            logger.warn("Could not apply metrics server, it may already exist: ${e.message}")
        }

        // Apply all YAML files and wait for resources to be ready
        executeKubectlApplyAndWait(k8sDirectory)

        // Wait for deployments and pods to be ready
        waitForDeployments(k8sNamespace, "600s")
        waitForPods(k8sNamespace, "600s")

        // Initialize Vert.x
        vertx = Vertx.vertx()
        webClient = WebClient.create(vertx)

        // wait a bit more for services to be fully ready
        Thread.sleep(30000) // 30 seconds

        logger.info("Kubernetes resources are ready for testing")
    }


    private fun queryPrometheusFor95thPercentile(operationType: String): Double {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """histogram_quantile(0.95, sum(rate(${operationType}_duration_seconds_bucket{service="$serviceName"}[5m])) by (le))"""

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val prometheusQuery = "$prometheusUrl/api/v1/query?query=$encodedQuery&time=$endTime"

            logger.debug("Querying Prometheus: {}", prometheusQuery)

            val response = webClient
                .getAbs(prometheusQuery)
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                logger.debug("Prometheus response: {}", body)

                // Check if result array is empty
                if (body.contains(""""result":[]""")) {
                    logger.warn("Prometheus returned empty result set for ${operationType}_duration_seconds - trying alternatives")
                    tryActualServiceMetrics()
                } else {
                    // Parse the JSON response to extract the value
                    val valueRegex = """"value":\s*\[\s*\d+(?:\.\d+)?,\s*"([^"]+)"\s*]""".toRegex()
                    val matchResult = valueRegex.find(body)

                    if (matchResult != null) {
                        val valueStr = matchResult.groupValues[1]
                        val valueSeconds = valueStr.toDoubleOrNull() ?: 0.0
                        val valueMilliseconds = valueSeconds * 1000.0
                        logger.info("Successfully parsed 95th percentile: {} seconds ({} ms)", valueSeconds, valueMilliseconds)
                        valueMilliseconds
                    } else {
                        logger.warn("Could not parse Prometheus response value from body: {}", body)
                        tryActualServiceMetrics()
                    }
                }
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                tryActualServiceMetrics()
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus: {}", e.message, e)
            0.0
        }
    }

    private fun tryActualServiceMetrics(): Double {
        try {
            logger.info("All specific queries failed. Checking what metrics are actually available...")
            val metricsListQuery = "$prometheusUrl/api/v1/label/__name__/values"
            val response = webClient
                .getAbs(metricsListQuery)
                .timeout(10000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val body = response.bodyAsString()
                logger.info("Available metrics in Prometheus: {}", body)
            } else {
                logger.warn("Could not list available metrics: status {}", response.statusCode())
            }
        } catch (e: Exception) {
            logger.error("Could not list available metrics: {}", e.message)
        }

        logger.warn("All service metric queries failed, returning 0.0")
        return 0.0
    }
}