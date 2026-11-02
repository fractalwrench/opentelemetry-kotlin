package io.opentelemetry.kotlin

import io.opentelemetry.kotlin.config.ConfigProvider
import io.opentelemetry.kotlin.context.ProcessWideRegistration
import io.opentelemetry.kotlin.context.ProcessWideRegistrationOwner
import io.opentelemetry.kotlin.context.createOtelJavaCurrentContextStorage
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.export.CompositeTelemetryCloseable
import io.opentelemetry.kotlin.export.MutableShutdownState
import io.opentelemetry.kotlin.export.OperationResultCode
import io.opentelemetry.kotlin.export.TelemetryCloseable
import io.opentelemetry.kotlin.factory.IdGenerator
import io.opentelemetry.kotlin.factory.ResourceFactory
import io.opentelemetry.kotlin.logging.LoggerProvider
import io.opentelemetry.kotlin.metrics.MeterProvider
import io.opentelemetry.kotlin.propagation.TextMapPropagator
import io.opentelemetry.kotlin.tracing.TracerProvider

internal class CompatOpenTelemetryImpl(
    override val tracerProvider: TracerProvider,
    override val loggerProvider: LoggerProvider,
    override val meterProvider: MeterProvider,
    override val clock: Clock,
    override val idGenerator: IdGenerator,
    override val resource: ResourceFactory,
    override val propagator: TextMapPropagator,
    sdkErrorHandler: SdkErrorHandler,
    override val processWideRegistration: ProcessWideRegistration? = null,
) : OpenTelemetrySdk, ProcessWideRegistrationOwner {

    // TODO: delegate to opentelemetry-java once it ships the Instrumentation Configuration API.
    override val configProvider: ConfigProvider = NoopOpenTelemetry.configProvider

    private val shutdownState: MutableShutdownState = MutableShutdownState()

    private val closeable = CompositeTelemetryCloseable(
        closeables = listOfNotNull(
            tracerProvider as? TelemetryCloseable,
            loggerProvider as? TelemetryCloseable,
            meterProvider as? TelemetryCloseable,
        ),
        sdkErrorHandler = sdkErrorHandler,
    )

    override suspend fun forceFlush(): OperationResultCode = when {
        shutdownState.isShutdown -> OperationResultCode.Success
        else -> closeable.forceFlush()
    }

    override suspend fun shutdown(): OperationResultCode = shutdownState.shutdown {
        closeable.shutdown().also { processWideRegistration?.release() }
    }
}

/**
 * Compat mode always shares the current context with opentelemetry-java, so it installs the Java
 * bridge storage. If another storage is already installed it is used instead, and Kotlin and Java
 * contexts are only shared if that storage is itself backed by the Java bridge.
 */
internal fun registerCompatProcessWideState(errorHandler: SdkErrorHandler): ProcessWideRegistration =
    ProcessWideRegistration.register(
        configured = createOtelJavaCurrentContextStorage(),
        default = ::createOtelJavaCurrentContextStorage,
        errorHandler = errorHandler,
        ignoredStorageMessage = "A CurrentContextStorage is already installed in this process, so compat mode uses " +
            "it. Kotlin and Java contexts are only shared if it is backed by createOtelJavaCurrentContextStorage().",
    )
