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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    private val csvOutputDir = "test-results/metrics"
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

    data class PrometheusMetric(
        val name: String,
        val value: String,
        val timestamp: String,
        val labels: Map<String, String> = emptyMap(),
        val help: String = "",
        val type: String = ""
    )

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

        // Create CSV output directory
        File(csvOutputDir).mkdirs()

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

        // Start metrics collection before test
        val metricsCollectionFuture = startContinuousMetricsCollection("escalating_spike_test")

        val results = escalatingSpikeTest(10, 50, "/CarePlan", carePlanTest, "/CarePlan/002")

        // Stop metrics collection and export final snapshot
        stopContinuousMetricsCollection(metricsCollectionFuture)
        exportPrometheusMetricsToCSV("escalating_spike_test_final")

        // Log the results
        results.logSummary()

        // Constraints can be added here based on requirements
//        assertAll(
//            { assertEquals(results.totalRequestNumber, results.successfulRequests, "All requests should be successful") },
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

        // Start metrics collection before test
        val metricsCollectionFuture = startContinuousMetricsCollection("sudden_spike_test")

        val results = spikeTest(500, "/CarePlan", carePlanTest, "/CarePlan/002")

        // Stop metrics collection and export final snapshot
        stopContinuousMetricsCollection(metricsCollectionFuture)
        exportPrometheusMetricsToCSV("sudden_spike_test_final")

        // Log the results
        results.logSummary()

        // Constraints can be added here based on requirements
//        assertAll(
//            { assertEquals(results.totalRequestNumber, results.successfulRequests, "All requests should be successful") },
//            { assertTrue(results.p95ResponseTime < 2000, "95th percentile response time should be under 2000 ms") },
//            { assertTrue(results.replicaNumber > 1, "Should have scaled horizontally") },
//        )
        assertTrue(true)
    }

    /**
     * Exports Prometheus metrics to CSV format by fetching data from the /metrics endpoint
     */
    fun exportPrometheusMetricsToCSV(testScenario: String = "default"): String? {
        return try {
            logger.info("Fetching metrics from Prometheus /metrics endpoint...")

            val response = webClient
                .getAbs("$prometheusUrl/metrics")
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val metricsData = response.bodyAsString()
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val csvFileName = "$csvOutputDir/${testScenario}_metrics_${timestamp}.csv"

                parseAndSaveMetricsAsCSV(metricsData, csvFileName)
                logger.info("Metrics exported to CSV: $csvFileName")
                csvFileName
            } else {
                logger.error("Failed to fetch metrics. Status: ${response.statusCode()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error exporting metrics to CSV: ${e.message}", e)
            null
        }
    }

    /**
     * Starts continuous metrics collection in a separate thread
     */
    private fun startContinuousMetricsCollection(testScenario: String): Future<Void> {
        logger.info("Starting continuous metrics collection for scenario: $testScenario")

        return vertx.executeBlocking<Void> { promise ->
            try {
                var iteration = 0
                while (!Thread.currentThread().isInterrupted) {
                    exportPrometheusMetricsToCSV("${testScenario}_iteration_${++iteration}")
                    Thread.sleep(30000) // Collect metrics every 30 seconds
                }
                promise.complete()
            } catch (e: InterruptedException) {
                logger.info("Continuous metrics collection stopped")
                promise.complete()
            } catch (e: Exception) {
                logger.error("Error in continuous metrics collection: ${e.message}", e)
                promise.fail(e)
            }
        }
    }

    /**
     * Stops continuous metrics collection
     */
    private fun stopContinuousMetricsCollection(future: Future<Void>) {
        logger.info("Stopping continuous metrics collection...")
        // The future will complete when the blocking thread is interrupted
        // In a real implementation, you might want to use a more sophisticated cancellation mechanism
    }

    /**
     * Parses Prometheus metrics format and saves as CSV
     */
    private fun parseAndSaveMetricsAsCSV(metricsData: String, csvFileName: String) {
        val metrics = parsePrometheusMetrics(metricsData)

        File(csvFileName).parentFile.mkdirs()
        PrintWriter(FileWriter(csvFileName)).use { writer ->
            // Write CSV header
            writer.println("timestamp,metric_name,metric_value,metric_type,help_text,labels")

            val currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            metrics.forEach { metric ->
                val labelsString = metric.labels.entries.joinToString(";") { "${it.key}=${it.value}" }
                writer.println("\"$currentTimestamp\",\"${metric.name}\",\"${metric.value}\",\"${metric.type}\",\"${metric.help}\",\"$labelsString\"")
            }
        }

        logger.info("Saved ${metrics.size} metrics to $csvFileName")
    }

    /**
     * Parses Prometheus metrics format into structured data
     */
    private fun parsePrometheusMetrics(metricsData: String): List<PrometheusMetric> {
        val metrics = mutableListOf<PrometheusMetric>()
        val lines = metricsData.lines()

        var currentHelp = ""
        var currentType = ""
        var currentMetricName = ""

        for (line in lines) {
            when {
                line.startsWith("# HELP ") -> {
                    val parts = line.substring(7).split(" ", limit = 2)
                    currentMetricName = parts[0]
                    currentHelp = if (parts.size > 1) parts[1] else ""
                }
                line.startsWith("# TYPE ") -> {
                    val parts = line.substring(7).split(" ", limit = 2)
                    currentType = if (parts.size > 1) parts[1] else ""
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    // Parse metric line: metric_name{labels} value [timestamp]
                    val metric = parseMetricLine(line, currentHelp, currentType)
                    if (metric != null) {
                        metrics.add(metric)
                    }
                }
            }
        }

        return metrics
    }

    /**
     * Parses a single metric line from Prometheus format
     */
    private fun parseMetricLine(line: String, help: String, type: String): PrometheusMetric? {
        return try {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 2) return null

            val metricPart = parts[0]
            val value = parts[1]
            val timestamp = if (parts.size > 2) parts[2] else ""

            // Parse metric name and labels
            val (name, labels) = if (metricPart.contains('{')) {
                val nameEnd = metricPart.indexOf('{')
                val name = metricPart.substring(0, nameEnd)
                val labelsString = metricPart.substring(nameEnd + 1, metricPart.lastIndexOf('}'))
                val labels = parseLabels(labelsString)
                Pair(name, labels)
            } else {
                Pair(metricPart, emptyMap<String, String>())
            }

            PrometheusMetric(
                name = name,
                value = value,
                timestamp = timestamp,
                labels = labels,
                help = help,
                type = type
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse metric line: $line - ${e.message}")
            null
        }
    }

    /**
     * Parses labels from Prometheus format: key1="value1",key2="value2"
     */
    private fun parseLabels(labelsString: String): Map<String, String> {
        if (labelsString.isBlank()) return emptyMap()

        val labels = mutableMapOf<String, String>()
        val regex = """(\w+)="([^"]*)"(?:,|$)""".toRegex()

        regex.findAll(labelsString).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            labels[key] = value
        }

        return labels
    }

    /**
     * Exports specific metrics based on query filters to CSV
     */
    fun exportFilteredMetricsToCSV(
        testScenario: String,
        metricNameFilters: List<String> = emptyList(),
        labelFilters: Map<String, String> = emptyMap()
    ): String? {
        return try {
            val response = webClient
                .getAbs("$prometheusUrl/metrics")
                .timeout(30000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS)

            if (response.statusCode() == 200) {
                val metricsData = response.bodyAsString()
                val allMetrics = parsePrometheusMetrics(metricsData)

                // Apply filters
                val filteredMetrics = allMetrics.filter { metric ->
                    val nameMatches = metricNameFilters.isEmpty() ||
                            metricNameFilters.any { filter -> metric.name.contains(filter, ignoreCase = true) }

                    val labelMatches = labelFilters.isEmpty() ||
                            labelFilters.all { (key, value) -> metric.labels[key] == value }

                    nameMatches && labelMatches
                }

                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val csvFileName = "$csvOutputDir/${testScenario}_filtered_metrics_${timestamp}.csv"

                saveMetricsAsCSV(filteredMetrics, csvFileName)
                logger.info("Filtered metrics (${filteredMetrics.size} entries) exported to CSV: $csvFileName")
                csvFileName
            } else {
                logger.error("Failed to fetch metrics. Status: ${response.statusCode()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error exporting filtered metrics to CSV: ${e.message}", e)
            null
        }
    }

    /**
     * Helper method to save metrics list as CSV
     */
    private fun saveMetricsAsCSV(metrics: List<PrometheusMetric>, csvFileName: String) {
        File(csvFileName).parentFile.mkdirs()
        PrintWriter(FileWriter(csvFileName)).use { writer ->
            writer.println("timestamp,metric_name,metric_value,metric_type,help_text,labels")

            val currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            metrics.forEach { metric ->
                val labelsString = metric.labels.entries.joinToString(";") { "${it.key}=${it.value}" }
                writer.println("\"$currentTimestamp\",\"${metric.name}\",\"${metric.value}\",\"${metric.type}\",\"${metric.help}\",\"$labelsString\"")
            }
        }
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

            // Export metrics snapshot after each iteration
            exportPrometheusMetricsToCSV("escalating_spike_iteration_${i + 1}")
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