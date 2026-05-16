package me.seroperson.reload.live.gradle

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class LiveReloadGrpcReflectionTest : LiveReloadTestBase() {
    @field:TempDir lateinit var projectDir: File

    private val appCode by lazy {
        val kotlinSources = projectDir.resolve("src/main/kotlin")
        kotlinSources.mkdirs()
        kotlinSources.resolve("App.kt")
    }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `reflection bidi streaming passthrough lists services`() {
        settingsFile.writeText(SETTINGS_CONTENT)
        buildFile.writeText(BUILD_CONTENT)
        appCode.writeText(APP_CODE)

        val runner = initGradleRunner(":liveReloadRun", projectDir)
        val isBuildRunning = AtomicBoolean(true)
        val runThread =
            Thread {
                try {
                    runner.build()
                    isBuildRunning.set(false)
                } catch (_: InterruptedException) {
                    println("Interrupted")
                } catch (ex: Exception) {
                    println("Got exception ${ex.message}")
                }
            }
        runThread.start()

        val ok = runReflectionUntil(isBuildRunning, "localhost", 9000, "grpc.health.v1.Health")

        runThread.interrupt()

        assertTrue(ok)
    }

    companion object {
        const val SETTINGS_CONTENT = ""
        const val BUILD_CONTENT =
            """
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    application
    id("me.seroperson.reload.live.gradle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.72.0")
    implementation("io.grpc:grpc-stub:1.72.0")
    implementation("io.grpc:grpc-protobuf:1.72.0")
    implementation("io.grpc:grpc-services:1.72.0")
}

liveReload {
    serverType = me.seroperson.reload.live.gradle.ServerType.GRPC
    settings = mapOf(
        "live.reload.grpc.port" to "8081",
        "live.reload.proxy.grpc.port" to "9000"
    )
}

application { mainClass = "AppKt" }
"""

        const val APP_CODE =
            """
import io.grpc.BindableService
import io.grpc.MethodDescriptor
import io.grpc.ServerBuilder
import io.grpc.ServerCallHandler
import io.grpc.ServerServiceDefinition
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.stub.ServerCalls
import java.io.ByteArrayInputStream
import java.io.InputStream

fun main() {
    val health = HealthStatusManager()
    val server = ServerBuilder
        .forPort(8081)
        .addService(GreeterService())
        .addService(health.healthService)
        .addService(ProtoReflectionService.newInstance())
        .build()
    try {
        server.start()
        health.setStatus("", HealthCheckResponse.ServingStatus.SERVING)
        println("Server started on port 8081")
        Thread.currentThread().join()
    } catch (ex: InterruptedException) {
        health.setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING)
        server.shutdown()
    }
}

class GreeterService : BindableService {
    override fun bindService(): ServerServiceDefinition {
        val methodDescriptor = MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("greeter.Greeter/Greet")
            .setRequestMarshaller(ByteArrayMarshaller())
            .setResponseMarshaller(ByteArrayMarshaller())
            .build()

        val handler: ServerCallHandler<ByteArray, ByteArray> = ServerCalls.asyncUnaryCall { _, responseObserver ->
            responseObserver.onNext("Hello".toByteArray())
            responseObserver.onCompleted()
        }

        return ServerServiceDefinition.builder("greeter.Greeter")
            .addMethod(methodDescriptor, handler)
            .build()
    }
}

class ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
    override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
    override fun parse(stream: InputStream): ByteArray = stream.readBytes()
}
"""
    }
}
