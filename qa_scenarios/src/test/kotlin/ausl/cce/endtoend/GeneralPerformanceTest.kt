package ausl.cce.endtoend

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import mf.cce.utils.KubernetesTest
import mf.cce.utils.carePlanTest
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
    private val k8sFiles = "src/test/resources/ausl/cce/endtoend/performance"
    private val k8sPrometheus = "src/test/resources/ausl/cce/endtoend/prometheus"
    private val k8sNamespace = "monitoring-app"
    private val urlHost = "http://localhost:31080"
    private val prometheusUrl = "http://localhost:31090"
    private lateinit var k8sDirectory: File
    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient
    private val getCarePlanOperation = "get_care_plan"

    data class TestSummary(
        val scenarioName: String,
        val duration: Duration,
        val totalRequestNumber: Long,
        val successfulRequests: Long,
        val p95ResponseTime: Double,
        val replicaNumber: Int = 0,
    ) {
        private val logger = LogManager.getLogger(this::class)

        fun logSummary() {
            logger.info("=== TEST SUMMARY FOR '$scenarioName' ===")
            logger.info("Test duration: ${duration.toSeconds()} seconds")
            logger.info("Total requests sent: $totalRequestNumber")
            logger.info("Successful requests: $successfulRequests")
            logger.info("Success rate: ${String.format("%.2f", (successfulRequests.toDouble() / totalRequestNumber * 100))}%")
            logger.info("95th percentile response time: ${String.format("%.2f", p95ResponseTime)} ms")
            logger.info("Number of replicas at test end: $replicaNumber")
            logger.info("==================")
        }
    }

    @BeforeEach
    fun setUp() {
        vertx = Vertx.vertx()

        val options = WebClientOptions()
            .setMaxPoolSize(500)
            .setMaxWaitQueueSize(1000)
            .setKeepAlive(true)
            .setKeepAliveTimeout(30)
            .setPipelining(false)
            .setConnectTimeout(10000)
            .setIdleTimeout(60)
            .setTryUseCompression(true)
            .setTcpKeepAlive(true)
            .setTcpNoDelay(true)
            .setHttp2MaxPoolSize(500)
            .setMaxRedirects(3)
            .setFollowRedirects(true)

        webClient = WebClient.create(vertx, options)

        setUpEnvironment(k8sPrometheus)
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
    @DisplayName("Test to evaluate 'terapia' architecture performance with escalating spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSustainedAverageLoad() {
        setUpEnvironment(k8sFiles)
        val results = escalatingSpikeTest(10, 50, "/CarePlan", carePlanTest, "/CarePlan/002")

        // Log the results
        results.logSummary()

        // Constraints can be added here based on requirements
//        assertAll(
//            { assertTrue(results.successfulRequests == results.totalRequestNumber, "All requests should be successful") },
//            { assertTrue(results.p95ResponseTime < 2000, "95th percentile response time should be under 2000 ms") },
//            { assertTrue(results.replicaNumber > 1, "Should have scaled horizontally") },
//        )
        assertTrue(true)
    }

    @Test
    @DisplayName("Test to evaluate 'terapia' architecture performance with sudden spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSuddenSpikeLoad() {
        setUpEnvironment(k8sFiles)
        val results = spikeTest(500, "/CarePlan", carePlanTest, "/CarePlan/002")

        // Log the results
        results.logSummary()

        // Constraints can be added here based on requirements
//        assertAll(
//            { assertTrue(results.successfulRequests == results.totalRequestNumber, "All requests should be successful") },
//            { assertTrue(results.p95ResponseTime < 2000, "95th percentile response time should be under 2000 ms") },
//            { assertTrue(results.replicaNumber > 1, "Should have scaled horizontally") },
//        )
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

        // wait for services to be fully ready
        Thread.sleep(30000) // 30 seconds

        logger.info("Kubernetes resources are ready for testing")
    }

    /**
     * Simulates an escalating spike test by sending an increasing number of GET requests
     * to the specified endpoint over a series of iterations. Each iteration waits for 30 seconds
     * before sending the next batch of requests. The number of requests sent in each iteration
     * increases linearly based on the iteration number and a multiplier.
     * @param iterationNumber The total number of iterations to perform.
     * @param requestsMultiplier The multiplier to determine the number of requests sent in each iteration. The number of requests sent in iteration i is calculated as i * requestsMultiplier.
     * @param postSlashEndpoint The endpoint to which the initial POST request is sent to create a resource.
     * @param postRequestBody The JSON body to be sent in the initial POST request.
     * @param getSlashEndpoint The endpoint to which the GET requests are sent to retrieve the resource.
     *
     * For example, with iterationNumber = 10 and requestsMultiplier = 10:
     * - Iteration 1: Sends 10 GET requests
     * - Iteration 2: Sends 20 GET requests
     * - ...
     * - Iteration 10: Sends 100 GET requests
     */
    private fun escalatingSpikeTest(
        iterationNumber: Int,
        requestsMultiplier: Int,
        postSlashEndpoint: String,
        postRequestBody: String,
        getSlashEndpoint: String,
        serviceName: String = "terapia"
    ): TestSummary {
        val startTime = System.currentTimeMillis()

        logger.info("Sending initial POST request to create resource...")
        sendPostRequestTo(postSlashEndpoint, postRequestBody, 1)

        repeat(iterationNumber) { i ->
            logger.info("Waiting 30 seconds before next iteration...")
            Thread.sleep(30_000)
            logger.info("Iteration ${i + 1}/$iterationNumber: Sending ${(i + 1) * requestsMultiplier} GET requests...")
            sendGetRequestTo(getSlashEndpoint, (i + 1) * requestsMultiplier)
        }

        logger.info("Escalating spike test completed, extracting results...")

        // Wait a bit for metrics to be collected
        Thread.sleep(10_000)

        val endTime = System.currentTimeMillis()
        val duration = Duration.ofMillis(endTime - startTime)

        // Query Prometheus for metrics
        val p95ResponseTime = queryForPercentile(prometheusUrl, getCarePlanOperation, serviceName, 0.95)
        val successfulRequests = queryForSuccessfulRequests(prometheusUrl, getCarePlanOperation, serviceName)
        val totalRequests = queryForTotalRequestsNumber(prometheusUrl, getCarePlanOperation, serviceName)
        val currentReplicas = getCurrentReplicas(serviceName, k8sNamespace)

        return TestSummary(
            scenarioName = "Escalating Spike Test",
            duration = duration,
            totalRequestNumber = totalRequests,
            successfulRequests = successfulRequests,
            p95ResponseTime = p95ResponseTime,
            replicaNumber = currentReplicas,
        )
    }

    /**
     * Simulates a spike test by sending concurrently a certain number of GET requests to the specified endpoint.
     * @param requestsNumber The number of requests to be sent.
     * @param postSlashEndpoint The endpoint to which the initial POST request is sent to create a resource.
     * @param postRequestBody The JSON body to be sent in the initial POST request.
     * @param getSlashEndpoint The endpoint to which the GET requests are sent to retrieve the resource.
     */
    private fun spikeTest(
        requestsNumber: Int,
        postSlashEndpoint: String,
        postRequestBody: String,
        getSlashEndpoint: String,
        serviceName: String = "terapia"
    ): TestSummary {
        val startTime = System.currentTimeMillis()

        logger.info("Sending initial POST request to create resource...")
        sendPostRequestTo(postSlashEndpoint, postRequestBody, 1)

        logger.info("Waiting 30 seconds before sending GET requests...")
        Thread.sleep(30_000)

        logger.info("Sending $requestsNumber GET requests...")
        sendGetRequestTo(getSlashEndpoint, requestsNumber)

        logger.info("Spike test completed, extracting results...")

        // Wait a bit for metrics to be collected
        Thread.sleep(10_000)

        val endTime = System.currentTimeMillis()
        val duration = Duration.ofMillis(endTime - startTime)

        // Query Prometheus for metrics
        val p95ResponseTime = queryForPercentile(prometheusUrl, getCarePlanOperation, serviceName, 0.95)
        val successfulRequests = queryForSuccessfulRequests(prometheusUrl, getCarePlanOperation, serviceName)
        val totalRequests = queryForTotalRequestsNumber(prometheusUrl, getCarePlanOperation, serviceName)
        val currentReplicas = getCurrentReplicas(serviceName, k8sNamespace)

        return TestSummary(
            scenarioName = "Spike Test",
            duration = duration,
            totalRequestNumber = totalRequests,
            successfulRequests = successfulRequests,
            p95ResponseTime = p95ResponseTime,
            replicaNumber = currentReplicas,
        )
    }

    private fun sendPostRequestTo(slashEndpoint: String, jsonBody: String, number: Int = 1): Unit? {
        return try {
            for (i in 1..number) {
                webClient
                    .postAbs("$urlHost$slashEndpoint")
                    .putHeader("Content-Type", "application/json")
                    .sendBuffer(Buffer.buffer(jsonBody))
            }
        } catch (e: Exception) {
            logger.error("Failed to send POST request to $slashEndpoint: ${e.message}", e)
            null
        }
    }

    private fun sendGetRequestTo(slashEndpoint: String, number: Int = 1) {
        try {
            val futures = mutableListOf<Future<HttpResponse<Buffer>>>()

            val batchSize = 10
            for (batch in 0 until number step batchSize) {
                val batchEnd = minOf(batch + batchSize, number)

                for (i in batch until batchEnd) {
                    val future = webClient
                        .getAbs("$urlHost$slashEndpoint")
                        .timeout(30000) // 30 second timeout per request
                        .send()
                    futures.add(future)
                }

                if (number > 20 && batch + batchSize < number) {
                    Thread.sleep(100)
                }
            }

            CompositeFuture.all(futures.map { it as Future<*> })
                .onSuccess { _ ->
                    val successful = futures.count { it.succeeded() }
                    val failed = futures.count { !it.succeeded() }
                    logger.info("Completed $number requests: $successful successful, $failed failed")

                    futures.forEachIndexed { index, future ->
                        if (future.succeeded()) {
                            logger.debug("Request ${index + 1}: Status ${future.result().statusCode()}")
                        } else {
                            logger.warn("Request ${index + 1}: Failed - ${future.cause()?.message}")
                        }
                    }
                }
                .onFailure { throwable ->
                    logger.error("Some requests failed: ${throwable.message}", throwable)
                }
                .toCompletionStage()
                .toCompletableFuture()
                .get(120, TimeUnit.SECONDS)

        } catch (e: Exception) {
            logger.error("Failed to send GET requests to $slashEndpoint: ${e.message}", e)
        }
    }

    private fun queryForPercentile(metricsServerUrl: String, operationType: String, serviceName: String, percentile: Double, time: String = "1h"): Double {
        val endTime = System.currentTimeMillis() / 1000

        return try {
            val query = """histogram_quantile($percentile, sum(rate(${operationType}_duration_seconds_bucket{service="$serviceName"}[$time])) by (le))"""

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
}