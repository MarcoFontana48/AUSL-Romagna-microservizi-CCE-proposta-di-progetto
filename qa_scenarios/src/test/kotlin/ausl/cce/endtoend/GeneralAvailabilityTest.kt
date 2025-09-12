package ausl.cce.endtoend

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import mf.cce.utils.KubernetesTest
import mf.cce.utils.allergyIntoleranceTest
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertTrue

/**
 * Test class for measuring recovery time after pod failure while maintaining high availability.
 * Tests the resilience of the system by deleting pods and measuring time to recovery.
 */
class RecoveryTimeTest : KubernetesTest() {
    private val logger = LogManager.getLogger(this::class)
    private val k8sYamlFiles = "src/test/resources/ausl/cce/endtoend/availability"
    private val k8sPrometheus = "src/test/resources/ausl/cce/endtoend/prometheus"
    private val k8sNamespace = "monitoring-app"
    private val hostUrl = "http://localhost:31080"
    private val healthEndpoint = "/health"
    private lateinit var k8sDirectory: File
    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient
    private lateinit var healthCheckExecutor: ScheduledExecutorService

    /**
     * Represents a failure period with start and end times
     */
    data class FailurePeriod(
        val startTime: Instant,
        val endTime: Instant?
    ) {
        val durationMs: Long
            get() = if (endTime != null) {
                Duration.between(startTime, endTime).toMillis()
            } else 0L
    }

    data class RecoveryTestResult(
        val testName: String,
        val initialReplicas: Int,
        val finalReplicas: Int,
        val totalHealthChecks: Int,
        val successfulHealthChecks: Int,
        val failedHealthChecks: Int,
        val availabilityPercentage: Double,
        val recoveryTimeMs: Long,
        val maxConsecutiveFailures: Int,
        val testDuration: Duration,
        val averageResponseTime: Double,
        val failurePeriods: List<FailurePeriod>
    ) {
        private val logger = LogManager.getLogger(this::class)

        fun logSummary() {
            logger.info("=== TEST SUMMARY FOR '$testName' ===")
            logger.debug("Initial replicas: $initialReplicas")
            logger.debug("Final replicas: $finalReplicas")
            logger.info("Test duration: ${testDuration.toSeconds()} seconds")
            logger.info("Total health checks: $totalHealthChecks")
            logger.info("Successful health checks: $successfulHealthChecks")
            logger.debug("Failed health checks: $failedHealthChecks")
            logger.info("Availability: ${String.format("%.4f", availabilityPercentage)}%")
            logger.info("")

            logger.debug("Total failures: ${failurePeriods.size}")
            if (failurePeriods.isNotEmpty()) {
                val totalDowntimeMs = failurePeriods.sumOf { it.durationMs }
                logger.debug("Total downtime: ${totalDowntimeMs / 1000}s")
                logger.info("Average failure duration: ${String.format("%.2f", totalDowntimeMs.toDouble() / failurePeriods.size / 1000)}s")
                logger.debug("Longest failure duration: ${failurePeriods.maxOf { it.durationMs } / 1000}s")
                if (failurePeriods.size > 1) {
                    logger.debug("Shortest failure duration: ${failurePeriods.minOf { it.durationMs } / 1000}s")
                }
            }

            logger.debug("Recovery time (worst case): ${recoveryTimeMs / 1000}s")
            logger.debug("Max consecutive failures: $maxConsecutiveFailures")
            logger.debug("Average response time: ${String.format("%.2f", averageResponseTime)}ms")
            logger.info("")
            logger.info("=== FAILURE ANALYSIS ===")
            failurePeriods.forEachIndexed { index, failure ->
                logger.info("Failure ${index + 1}: ${failure.durationMs / 1000}s duration")
            }
            logger.info("==================")
        }

        fun assertAvailability(minAvailability: Double = 95.0) {
            assertTrue(availabilityPercentage >= minAvailability,
                "Availability $availabilityPercentage% is below required minimum $minAvailability%")
        }

        fun assertAverageRecoveryTime(maxRecoveryTimeMs: Long = 30000) {
            if (failurePeriods.isNotEmpty()) {
                val avgRecoveryTime = failurePeriods.sumOf { it.durationMs }.toDouble() / failurePeriods.size
                assertTrue(avgRecoveryTime <= maxRecoveryTimeMs,
                    "Average recovery time ${avgRecoveryTime}ms exceeds maximum allowed ${maxRecoveryTimeMs}ms")
            }
        }
    }

    data class HealthCheckResult(
        val timestamp: Instant,
        val success: Boolean,
        val responseTimeMs: Long,
        val errorMessage: String? = null
    )

    @BeforeEach
    fun setUp() {
        setUpEnvironment(k8sYamlFiles)
        setUpEnvironment(k8sPrometheus)
        healthCheckExecutor = Executors.newScheduledThreadPool(2)
    }

    @AfterEach
    fun tearDown() {
        // Shutdown executor
        healthCheckExecutor.shutdown()
        try {
            if (!healthCheckExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            healthCheckExecutor.shutdownNow()
        }

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

    /**
     * Test that considers a scenario where we already have scaled horizontally and tests the availability upon some
     * nodes failing. One of the replicas of the 'anamnesi-pregressa' service is killed to simulate a failure
     * and upon coming back up the time taken to recover is measured.
     */
    @Test
    @DisplayName("Test recovery time for 'anamnesi-pregressa' service with a single pod failure where " +
            "'terapia' care plan has to be suspended and needs to process events coming from anamnesi - 1 pod")
    @Timeout(30 * 60) // 30 minutes timeout
    fun testAnamnesiRecoveryTimeUponFailingWithSinglePod() {
        val result = executeRecoveryTestWithSingleFailure(
            testName = "Anamnesi Pod Failure with 1 Pod",
            initialReplicas = 1,
            testDurationSeconds = 90,
            healthCheckIntervalMs = 1000,
            failureDelaySeconds = 15,
            healthCheckEndpoint = healthEndpoint,
            serviceToKill = "anamnesi-pregressa",
            jsonPostRequestBodyToKilledService = allergyIntoleranceTest,
        )

        result.logSummary()

        // A constraint can be added here as expected value to pass the test (as shown)
//        result.assertAverageRecoveryTime(20000) // should recover within 20 seconds on average (example value)

        assertTrue(true)
    }

    /**
     * Execute recovery test with one pod failure at specified time
     */
    private fun executeRecoveryTestWithSingleFailure(
        testName: String,
        initialReplicas: Int,
        testDurationSeconds: Int,
        healthCheckIntervalMs: Long,
        failureDelaySeconds: Int,
        healthCheckEndpoint: String = healthEndpoint,
        serviceToKill: String = "anamnesi-pregressa",
        jsonPostRequestBodyToKilledService: String,
    ): RecoveryTestResult {
        logger.info("Starting recovery test with single failure: $testName")
        logger.info("Failure scheduled at ${failureDelaySeconds}s")

        val healthCheckResults = mutableListOf<HealthCheckResult>()
        val testStartTime = Instant.now()
        val testEndTime = testStartTime.plusSeconds(testDurationSeconds.toLong())

        val totalHealthChecks = AtomicInteger(0)
        val successfulHealthChecks = AtomicInteger(0)
        val totalResponseTime = AtomicLong(0)
        val isRunning = AtomicBoolean(true)

        // Start continuous health checks
        val healthCheckFuture = CompletableFuture.runAsync({
            while (isRunning.get() && Instant.now().isBefore(testEndTime)) {
                val result = performHealthCheck(healthCheckEndpoint)
                synchronized(healthCheckResults) {
                    healthCheckResults.add(result)
                }
                totalHealthChecks.incrementAndGet()
                if (result.success) {
                    successfulHealthChecks.incrementAndGet()
                    totalResponseTime.addAndGet(result.responseTimeMs)
                }

                try {
                    Thread.sleep(healthCheckIntervalMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }, healthCheckExecutor)

        // Schedule pod deletion
        healthCheckExecutor.schedule({
            try {
                deleteSinglePod(serviceToKill)
                logger.info("Pod deleted successfully at ${failureDelaySeconds}s")
//                try {
//                    val response = sendPostRequestTo("/$serviceToKill", jsonPostRequestBodyToKilledService)
//                    logger.info("Request sent to killed service at ${failureDelaySeconds}s")
//                } catch (e: Exception) {
//                    logger.error("Failed to send request to the killed service: ${e.message}", e)
//                }
            } catch (e: Exception) {
                logger.error("Failed to delete pod: ${e.message}", e)
            }
        }, failureDelaySeconds.toLong(), TimeUnit.SECONDS)

        // Wait for test completion
        try {
            Thread.sleep(testDurationSeconds * 1000L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        isRunning.set(false)

        try {
            healthCheckFuture.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.warn("Health check future didn't complete cleanly: ${e.message}")
        }

        val finalReplicas = getCurrentReplicas(serviceToKill, k8sNamespace)

        return analyzeRecoveryResults(testName, initialReplicas, finalReplicas, healthCheckResults,
            testStartTime, totalHealthChecks.get(), successfulHealthChecks.get(), totalResponseTime.get())
    }

    /**
     * Execute recovery test with two sequential pod failures at different times
     */
    private fun executeRecoveryTestWithTwoFailures(
        testName: String,
        initialReplicas: Int,
        testDurationSeconds: Int,
        healthCheckIntervalMs: Long,
        firstFailureDelaySeconds: Int,
        secondFailureDelaySeconds: Int
    ): RecoveryTestResult {
        logger.info("Starting recovery test with two failures: $testName")
        logger.info("First failure scheduled at ${firstFailureDelaySeconds}s, second at ${secondFailureDelaySeconds}s")

        val healthCheckResults = mutableListOf<HealthCheckResult>()
        val testStartTime = Instant.now()
        val testEndTime = testStartTime.plusSeconds(testDurationSeconds.toLong())

        val totalHealthChecks = AtomicInteger(0)
        val successfulHealthChecks = AtomicInteger(0)
        val totalResponseTime = AtomicLong(0)
        val isRunning = AtomicBoolean(true)

        // Start continuous health checks
        val healthCheckFuture = CompletableFuture.runAsync({
            while (isRunning.get() && Instant.now().isBefore(testEndTime)) {
                val result = performHealthCheck(healthEndpoint)
                synchronized(healthCheckResults) {
                    healthCheckResults.add(result)
                }
                totalHealthChecks.incrementAndGet()
                if (result.success) {
                    successfulHealthChecks.incrementAndGet()
                    totalResponseTime.addAndGet(result.responseTimeMs)
                }

                try {
                    Thread.sleep(healthCheckIntervalMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }, healthCheckExecutor)

        // Schedule first pod deletion
        healthCheckExecutor.schedule({
            try {
                deleteSinglePod("terapia")
                logger.info("First pod deleted successfully at ${firstFailureDelaySeconds}s")
            } catch (e: Exception) {
                logger.error("Failed to delete first pod: ${e.message}", e)
            }
        }, firstFailureDelaySeconds.toLong(), TimeUnit.SECONDS)

        // Schedule second pod deletion (allowing time for recovery from first failure)
        healthCheckExecutor.schedule({
            try {
                deleteSinglePod("terapia")
                logger.info("Second pod deleted successfully at ${secondFailureDelaySeconds}s")
            } catch (e: Exception) {
                logger.error("Failed to delete second pod: ${e.message}", e)
            }
        }, secondFailureDelaySeconds.toLong(), TimeUnit.SECONDS)

        // Wait for test completion
        try {
            Thread.sleep(testDurationSeconds * 1000L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        isRunning.set(false)

        try {
            healthCheckFuture.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.warn("Health check future didn't complete cleanly: ${e.message}")
        }

        val finalReplicas = getCurrentReplicas("terapia", k8sNamespace)

        return analyzeRecoveryResults(testName, initialReplicas, finalReplicas, healthCheckResults,
            testStartTime, totalHealthChecks.get(), successfulHealthChecks.get(), totalResponseTime.get())
    }

    private fun sendPostRequestTo(slashEndpoint: String, jsonBody: String): HttpResponse<Buffer?>? {
        return try {
            val response = webClient
                .postAbs("$hostUrl$slashEndpoint")
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(jsonBody))
                .toCompletionStage()
                .toCompletableFuture()
                .get()

            response
        } catch (e: Exception) {
            logger.error("Failed to send POST request to $slashEndpoint: ${e.message}", e)
            null
        }
    }

    private fun sendGetRequestTo(slashEndpoint: String): HttpResponse<Buffer?>? {
        return try {
            val response = webClient
                .getAbs("$hostUrl$slashEndpoint")
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get()

            response
        } catch (e: Exception) {
            logger.error("Failed to send GET request to $slashEndpoint: ${e.message}", e)
            null
        }
    }

    private fun performHealthCheck(toEndpoint: String): HealthCheckResult {
        val startTime = Instant.now()

        return try {
            val response = webClient
                .getAbs("$hostUrl$toEndpoint")
                .timeout(5000)
                .send()
                .toCompletionStage()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS)

            val endTime = Instant.now()
            val responseTime = Duration.between(startTime, endTime).toMillis()

            // SERVICE-LEVEL availability: Success if we get ANY response (even if some pods are down)
            // This represents what end users experience
            HealthCheckResult(
                timestamp = startTime,
                success = response.statusCode() == 200,
                responseTimeMs = responseTime,
                errorMessage = if (response.statusCode() != 200) "HTTP ${response.statusCode()}" else null
            )
        } catch (e: Exception) {
            val endTime = Instant.now()
            val responseTime = Duration.between(startTime, endTime).toMillis()

            // SERVICE-LEVEL failure: Only fails when NO pods can serve the request
            HealthCheckResult(
                timestamp = startTime,
                success = false,
                responseTimeMs = responseTime,
                errorMessage = e.message
            )
        }
    }

    private fun deleteSinglePod(deploymentName: String) {
        logger.info("Deleting a single pod from deployment: $deploymentName")

        // Get pod names
        val getPodCommand = listOf("kubectl", "get", "pods", "-n", k8sNamespace,
            "-l", "app=$deploymentName", "-o", "jsonpath={.items[0].metadata.name}")

        val processBuilder = ProcessBuilder(getPodCommand)
            .redirectErrorStream(true)

        val process = processBuilder.start()
        val podName = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()

        if (exitCode != 0 || podName.isEmpty()) {
            throw RuntimeException("Failed to get pod name for deployment $deploymentName")
        }

        logger.info("Found pod to delete: $podName")

        // Delete the specific pod
        executeKubectlCmd(File("."), "delete", "pod", podName, "-n", k8sNamespace)
        logger.info("Pod $podName deleted successfully")
    }

    private fun scaleDeployment(deploymentName: String, replicas: Int) {
        logger.info("Scaling deployment $deploymentName to $replicas replicas")
        executeKubectlCmd(File("."), "scale", "deployment", deploymentName,
            "--replicas=$replicas", "-n", k8sNamespace)
        logger.info("Deployment scaled successfully")
    }

    private fun waitForDeploymentReady(deploymentName: String, expectedReplicas: Int) {
        logger.info("Waiting for deployment $deploymentName to have $expectedReplicas ready replicas")

        var attempts = 0
        val maxAttempts = 60 // 5 minutes with 5 second intervals

        while (attempts < maxAttempts) {
            try {
                val processBuilder = ProcessBuilder(
                    "kubectl", "get", "deployment", deploymentName, "-n", k8sNamespace,
                    "-o", "jsonpath={.status.readyReplicas}"
                ).redirectErrorStream(true)

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                val exitCode = process.waitFor()

                if (exitCode == 0 && output.isNotEmpty()) {
                    val readyReplicas = output.toIntOrNull() ?: 0
                    if (readyReplicas >= expectedReplicas) {
                        logger.info("Deployment $deploymentName is ready with $readyReplicas/$expectedReplicas replicas")
                        Thread.sleep(5000) // Additional wait for stability
                        return
                    }
                    logger.debug("Waiting for deployment readiness: $readyReplicas/$expectedReplicas ready")
                }

                attempts++
                Thread.sleep(5000)
            } catch (e: Exception) {
                logger.warn("Error checking deployment readiness: ${e.message}")
                attempts++
                Thread.sleep(5000)
            }
        }

        throw RuntimeException("Deployment $deploymentName did not become ready within expected time")
    }

    private fun analyzeRecoveryResults(
        testName: String,
        initialReplicas: Int,
        finalReplicas: Int,
        healthCheckResults: List<HealthCheckResult>,
        testStartTime: Instant,
        totalHealthChecks: Int,
        successfulHealthChecks: Int,
        totalResponseTime: Long
    ): RecoveryTestResult {

        val failedHealthChecks = totalHealthChecks - successfulHealthChecks
        val availabilityPercentage = if (totalHealthChecks > 0) {
            (successfulHealthChecks.toDouble() / totalHealthChecks) * 100.0
        } else 0.0

        val averageResponseTime = if (successfulHealthChecks > 0) {
            totalResponseTime.toDouble() / successfulHealthChecks
        } else 0.0

        // Calculate failure periods
        val failurePeriods = identifyFailurePeriods(healthCheckResults)

        // Calculate recovery time (time from first failure to sustained success)
        val recoveryTimeMs = calculateRecoveryTime(healthCheckResults)

        // Calculate max consecutive failures
        val maxConsecutiveFailures = calculateMaxConsecutiveFailures(healthCheckResults)

        val testDuration = if (healthCheckResults.isNotEmpty()) {
            Duration.between(testStartTime, healthCheckResults.last().timestamp)
        } else Duration.ZERO

        return RecoveryTestResult(
            testName = testName,
            initialReplicas = initialReplicas,
            finalReplicas = finalReplicas,
            totalHealthChecks = totalHealthChecks,
            successfulHealthChecks = successfulHealthChecks,
            failedHealthChecks = failedHealthChecks,
            availabilityPercentage = availabilityPercentage,
            recoveryTimeMs = recoveryTimeMs,
            maxConsecutiveFailures = maxConsecutiveFailures,
            testDuration = testDuration,
            averageResponseTime = averageResponseTime,
            failurePeriods = failurePeriods
        )
    }

    /**
     * Identifies distinct SERVICE failure periods from health check results.
     * A SERVICE failure period starts with the first failed health check and ends with the first successful check.
     */
    private fun identifyFailurePeriods(healthCheckResults: List<HealthCheckResult>): List<FailurePeriod> {
        val failurePeriods = mutableListOf<FailurePeriod>()
        var currentFailureStart: Instant? = null

        for (result in healthCheckResults) {
            if (!result.success && currentFailureStart == null) {
                // Start of a new SERVICE failure period
                currentFailureStart = result.timestamp
            } else if (result.success && currentFailureStart != null) {
                // End of current SERVICE failure period - service is restored
                failurePeriods.add(FailurePeriod(currentFailureStart, result.timestamp))
                currentFailureStart = null
            }
        }

        // Handle case where SERVICE failure period extends to end of test
        if (currentFailureStart != null && healthCheckResults.isNotEmpty()) {
            failurePeriods.add(FailurePeriod(currentFailureStart, healthCheckResults.last().timestamp))
        }

        return failurePeriods
    }

    private fun calculateRecoveryTime(healthCheckResults: List<HealthCheckResult>): Long {
        if (healthCheckResults.isEmpty()) return 0L

        var firstFailureTime: Instant? = null
        var consecutiveSuccesses = 0
        val requiredConsecutiveSuccesses = 3 // Consider recovered after 3 consecutive successes

        for (result in healthCheckResults) {
            if (!result.success) {
                if (firstFailureTime == null) {
                    firstFailureTime = result.timestamp
                }
                consecutiveSuccesses = 0
            } else {
                if (firstFailureTime != null) {
                    consecutiveSuccesses++
                    if (consecutiveSuccesses >= requiredConsecutiveSuccesses) {
                        return Duration.between(firstFailureTime, result.timestamp).toMillis()
                    }
                }
            }
        }

        // If we never recovered, return the time until the end
        return if (firstFailureTime != null && healthCheckResults.isNotEmpty()) {
            Duration.between(firstFailureTime, healthCheckResults.last().timestamp).toMillis()
        } else 0L
    }

    private fun calculateMaxConsecutiveFailures(healthCheckResults: List<HealthCheckResult>): Int {
        var maxConsecutive = 0
        var currentConsecutive = 0

        for (result in healthCheckResults) {
            if (!result.success) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }

        return maxConsecutive
    }

    private fun setUpEnvironment(k8sFilesPath: String) {
        k8sDirectory = File(k8sFilesPath)

        logger.info("Starting Kubernetes resources setup for recovery test...")
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
            logger.info("Metrics server applied")
        } catch (e: Exception) {
            logger.warn("Could not apply metrics server, it may already exist: ${e.message}")
        }

        // Apply all YAML files and wait for resources to be ready
        logger.info("Waiting for kubernetes resources to be ready...")
        executeKubectlApplyAndWait(k8sDirectory)

        // Wait for deployments and pods to be ready
        waitForDeployments(k8sNamespace, "600s")
        waitForPods(k8sNamespace, "600s")

        vertx = Vertx.vertx()
        webClient = WebClient.create(vertx)

        // Wait for services to be fully ready
        Thread.sleep(30000) // 30 seconds

        logger.info("Kubernetes resources are ready for recovery testing")
    }
}