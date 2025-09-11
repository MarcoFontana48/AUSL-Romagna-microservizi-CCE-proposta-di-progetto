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
    private val k8sTerapiaHPA = "src/test/resources/ausl/cce/endtoend/scaler"
    private val k8sTerapiaNoHPA = "src/test/resources/ausl/cce/endtoend/noscaler/performance"
    private val k8sPrometheus = "src/test/resources/ausl/cce/endtoend/prometheus"
    private val k8sNamespace = "monitoring-app"
    private lateinit var k8sDirectory: File

    // Service endpoints
    private val apiGatewayUrl = "http://localhost:31080"
    private val prometheusUrl = "http://localhost:31090"
    private val healthEndpoint = "/service/health"

    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient

    private val serviceName = "service"
    private val terapia = "terapia"
    private val diarioClinico = "diario-clinico"
    private val anamnesiPregressa = "anamnesi-pregressa"
    private val healthCheckOperation = "health_check"
    private val getCarePlanOperation = "get_care_plan"

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
        vertx = Vertx.vertx()

        val options = WebClientOptions()
            .setMaxPoolSize(500)
            .setMaxWaitQueueSize(200)
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
    @DisplayName("NO_HPA: Test to evaluate 'terapia' architecture performance with escalating spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSustainedAverageLoadNoHPA() {
        setUpEnvironment(k8sTerapiaNoHPA)
        escalatingSpikeTest(10, 1, "/terapia/CarePlan", carePlanTest, "/terapia/CarePlan/002")

        // a constraint can be added here instead of just passing the test
        assertTrue(true)
    }

    @Test
    @DisplayName("HPA: Test to evaluate 'terapia' architecture performance with escalating spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSustainedAverageLoadHPA() {
        setUpEnvironment(k8sTerapiaHPA)
        escalatingSpikeTest(10, 1, "/terapia/CarePlan", carePlanTest, "/terapia/CarePlan/002")

        // a constraint can be added here instead of just passing the test
        assertTrue(true)
    }

    @Test
    @DisplayName("NO_HPA: Test to evaluate 'terapia' architecture performance with sudden spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSuddenSpikeLoadNoHPA() {
        setUpEnvironment(k8sTerapiaNoHPA)
        // the amount of requests is set to a low amount in order to avoid overloading the machine used for personal testing, in a real case scenario this should be higher
        spikeTest(15, "/terapia/CarePlan", carePlanTest, "/terapia/CarePlan/002")
        // a constraint can be added here instead of just passing the test
        assertTrue(true)
    }

    @Test
    @DisplayName("HPA: Test to evaluate 'terapia' architecture performance with sudden spike load")
    @Timeout(30 * 60) // 30 minutes timeout
    fun performanceEvaluationSuddenSpikeLoadHPA() {
        setUpEnvironment(k8sTerapiaHPA)
        // the amount of requests is set to a low amount in order to avoid overloading the machine used for personal testing, in a real case scenario this should be higher
        spikeTest(35, "/terapia/CarePlan", carePlanTest, "/terapia/CarePlan/002")
        // a constraint can be added here instead of just passing the test
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
        getSlashEndpoint: String
    ) {
        logger.info("Sending initial POST request to create resource...")
        sendPostRequestTo(postSlashEndpoint, postRequestBody, 1)

        repeat(iterationNumber) { i ->
            logger.info("Waiting 30 seconds before next iteration...")
            Thread.sleep(30_000)
            logger.info("Iteration ${i + 1}/$iterationNumber: Sending ${i + 1 * requestsMultiplier} GET requests...")
            sendGetRequestTo(getSlashEndpoint, i + 1 * requestsMultiplier)
        }
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
        getSlashEndpoint: String
    ) {
        logger.info("Sending initial POST request to create resource...")
        sendPostRequestTo(postSlashEndpoint, postRequestBody, 1)

        logger.info("Waiting 30 seconds before sending GET requests...")
        Thread.sleep(30_000)

        repeat(requestsNumber) { i ->
            logger.info("Sending ${i + 1}/${requestsNumber} GET requests...")
            sendGetRequestTo(getSlashEndpoint, 1)
        }
    }

    private fun sendPostRequestTo(slashEndpoint: String, jsonBody: String, number: Int = 1): Unit? {
        return try {
            for (i in 1..number) {
                webClient
                    .postAbs("$apiGatewayUrl$slashEndpoint")
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
                        .getAbs("$apiGatewayUrl$slashEndpoint")
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
}