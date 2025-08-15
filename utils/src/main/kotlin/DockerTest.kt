package mf.cce.utils

import org.apache.logging.log4j.LogManager
import java.io.File

/**
 * Abstract class to be extended by tests that need to start docker-compose services.
 */
abstract class DockerTest {
    private val logger = LogManager.getLogger(this::class)

    fun executeDockerComposeUp(composeFile: File) {
        logger.trace("Starting docker-compose with file: {}", composeFile.absolutePath)
        executeDockerComposeCmd(composeFile, "up", "--wait")
        logger.trace("Docker-compose started successfully.")
    }

    fun executeDockerComposeDown(composeFile: File) {
        logger.trace("Stopping docker-compose with file: {}", composeFile.absolutePath)
        executeDockerComposeCmd(composeFile, "down", "-v")
        logger.trace("Docker-compose stopped successfully.")
    }

    fun executeDockerComposeCmd(composeFile: File, vararg arguments: String) {
        logger.trace("Searching file: '{}' at path: '{}'", composeFile, composeFile.absolutePath)

        if (!composeFile.exists()) {
            logger.error("File not found: {}", composeFile.absolutePath)
            throw IllegalStateException("File not found: ${composeFile.absolutePath}")
        }

        logger.trace("File found, executing docker-compose command with file: {}", composeFile.absolutePath)
        val command = mutableListOf("docker", "compose", "-f", composeFile.absolutePath)
        command.addAll(arguments)
        logger.trace("Executing commands: {}", command.joinToString(" "))

        val processBuilder = ProcessBuilder()
            .command(command)
            .redirectErrorStream(true)
            .directory(composeFile.parentFile)

        logger.trace("Starting process with command: {}", command.joinToString(" "))
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().use { it.readText() } // Capture output
        logger.trace("Process output: {}", output)

        logger.trace("Waiting for process to finish...")
        val exitCode = process.waitFor()
        logger.trace("Process exited with code: {}", exitCode)

        if (exitCode != 0) {
            logger.error("Error when starting docker-compose: $output")
            throw RuntimeException("Failed to start docker-compose: Exit code $exitCode, output: $output")
        }
    }
}