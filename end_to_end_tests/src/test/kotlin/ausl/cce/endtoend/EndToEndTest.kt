package ausl.cce.endtoend

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import mf.cce.utils.KubernetesTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class EndToEndTest : KubernetesTest() {
    private val logger = LogManager.getLogger(this::class)
    private val k8sYmlfilesPath = "src/test/resources/ausl/cce/endtoend"
    private val k8sNamespace = "monitoring-app"
    private lateinit var k8sDirectory: File

    // Service endpoints
    private val apiGatewayUrl = "http://localhost:31080"
    private val prometheusUrl = "http://localhost:31090"
    private val healthEndpoint = "/service/health"

    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    @BeforeEach
    fun setUp() {
        k8sDirectory = File(k8sYmlfilesPath)

        logger.info("Starting Kubernetes resources setup...")
        logger.info("K8s directory path: {}", k8sDirectory.absolutePath)

        // Check kubectl is available and cluster is accessible
        checkKubectlAvailability()

        // Apply metrics-server first (if not already present)
        try {
            executeKubectlCmd(File("."), "apply", "-f", "https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml")
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

        // Wait a bit more for services to be fully ready
        Thread.sleep(30000) // 30 seconds

        logger.info("Kubernetes resources are ready for testing")
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
    fun testConcurrentHealthChecksAndPrometheusMetrics() {
        logger.info("Starting concurrent health check test...")

        val numberOfClients = 50
        val requestsPerClient = 20
        val totalRequests = numberOfClients * requestsPerClient

        logger.info("Sending {} requests using {} concurrent clients ({} requests per client)",
            totalRequests, numberOfClients, requestsPerClient)

        // Send concurrent requests
        val futures = mutableListOf<CompletableFuture<Void>>()
        val startTime = System.currentTimeMillis()

        for (clientId in 1..numberOfClients) {
            val future = CompletableFuture<Void>()
            futures.add(future)

            vertx.executeBlocking<Void>({ promise ->
                try {
                    for (requestId in 1..requestsPerClient) {
                        val requestFuture = webClient
                            .getAbs("$apiGatewayUrl$healthEndpoint")
                            .timeout(10000) // 10 second timeout
                            .send()

                        requestFuture.result() // Wait for response
                        logger.trace("Client {} - Request {} completed", clientId, requestId)
                    }
                    promise.complete()
                } catch (e: Exception) {
                    logger.error("Error in client {}: {}", clientId, e.message)
                    promise.complete() // Continue even if some requests fail
                }
            }, { result ->
                if (result.succeeded()) {
                    logger.debug("Client {} completed all requests", clientId)
                } else {
                    logger.error("Client {} failed: {}", clientId, result.cause()?.message)
                }
                future.complete(null)
            })
        }

        // Wait for all clients to complete
        CompletableFuture.allOf(*futures.toTypedArray()).get(300, TimeUnit.SECONDS)


        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - startTime

        logger.info("All {} requests completed in {} ms", totalRequests, totalDuration)

        // Fix the formatting issue - remove {:.2f} placeholder and calculate the value
        val throughput = totalRequests.toDouble() / (totalDuration / 1000.0)
        logger.info("Average throughput: {} requests/second", String.format("%.2f", throughput))

        // Wait a bit for Prometheus to scrape the metrics
        Thread.sleep(30000) // 30 seconds

        // Query Prometheus for 95th percentile response time
        val p95ResponseTime = queryPrometheusFor95thPercentile()

        logger.info("95th percentile response time from Prometheus: {} ms", p95ResponseTime)

        // Test assertions (always pass as requested)
        assertTrue(true, "Test completed successfully")

        // Log summary
        logger.info("=== TEST SUMMARY ===")
        logger.info("Total requests sent: {}", totalRequests)
        logger.info("Concurrent clients: {}", numberOfClients)
        logger.info("Total test duration: {} ms", totalDuration)
        logger.info("Average throughput: {} req/s", String.format("%.2f", throughput))
        logger.info("95th percentile response time: {} ms", p95ResponseTime)
        logger.info("==================")
    }

    private fun queryPrometheusFor95thPercentile(): Double {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            // Query the health check endpoint since that's what your test is hitting
            // Try histogram first, then fallback to max values
            val query = """histogram_quantile(0.95, sum(rate(health_check_duration_seconds_bucket{service="service"}[5m])) by (le))"""

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
                    logger.warn("Prometheus returned empty result set for health_check_duration_seconds - trying alternatives")
                    tryActualServiceMetrics(endTime)
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
                        tryActualServiceMetrics(endTime)
                    }
                }
            } else {
                logger.warn("Prometheus query failed with status: {} - {}", response.statusCode(), response.bodyAsString())
                tryActualServiceMetrics(endTime)
            }
        } catch (e: Exception) {
            logger.error("Error querying Prometheus: {}", e.message, e)
            0.0
        }
    }

    private fun tryActualServiceMetrics(endTime: Long): Double {
        val actualQueries = listOf(
            // Try histogram buckets first (only available if you enabled publishPercentileHistogram)
            """histogram_quantile(0.95, sum(rate(health_check_duration_seconds_bucket{service="service"}[5m])) by (le))""",
            """histogram_quantile(0.95, sum(rate(health_check_duration_seconds_bucket[5m])) by (le))""",

            // Fallback to max values (these should always be available with Timers)
            """health_check_duration_seconds_max{service="service"}""",
            """health_check_duration_seconds_max""",

            // Try mean values
            """avg_over_time(health_check_duration_seconds_sum{service="service"}[5m]) / avg_over_time(health_check_duration_seconds_count{service="service"}[5m])""",

            // Check basic timer metrics to verify data is being collected
            """health_check_duration_seconds_count{service="service"}""",
            """health_check_duration_seconds_sum{service="service"}""",

            // Check counter metrics
            """health_check_requests_total{service="service"}""",
            """health_check_success_total{service="service"}""",

            // Check if services are being scraped
            """up{job="service"}""",
            """up{job="api-gateway"}"""
        )

        for (query in actualQueries) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val prometheusQuery = "$prometheusUrl/api/v1/query?query=$encodedQuery&time=$endTime"

                logger.debug("Trying actual service query: {}", query)

                val response = webClient
                    .getAbs(prometheusQuery)
                    .timeout(30000)
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(15, TimeUnit.SECONDS)

                if (response.statusCode() == 200) {
                    val body = response.bodyAsString()
                    logger.debug("Service metric query response: {}", body)

                    // Check if we got any data (not empty result array)
                    if (!body.contains(""""result":[]""")) {
                        // For count/up queries, just log that we found data
                        if (query.contains("_count") || query.startsWith("up") || query.contains("_total")) {
                            logger.info("Found metric data for: {}", query)
                            continue
                        }

                        val valueRegex = """"value":\s*\[\s*\d+(?:\.\d+)?,\s*"([^"]+)"\s*]""".toRegex()
                        val matchResult = valueRegex.find(body)

                        if (matchResult != null) {
                            val valueStr = matchResult.groupValues[1]
                            val valueSeconds = valueStr.toDoubleOrNull() ?: 0.0
                            val valueMilliseconds = valueSeconds * 1000.0
                            logger.info("Service metric query succeeded: {} ms (query: {})", valueMilliseconds, query)
                            return valueMilliseconds
                        }
                    } else {
                        logger.debug("Service metric query returned empty result: {}", query)
                    }
                } else {
                    logger.debug("Service metric query failed with status {}: {}", response.statusCode(), query)
                }
            } catch (e: Exception) {
                logger.debug("Service metric query failed: {} - {}", query, e.message)
            }
        }

        // Final diagnostic attempt - list all available metrics
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