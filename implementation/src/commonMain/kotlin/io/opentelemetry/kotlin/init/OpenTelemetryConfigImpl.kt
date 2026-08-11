package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.Clock
import io.opentelemetry.kotlin.config.dsl.AttributeLimitsConfigModelBuilder
import io.opentelemetry.kotlin.config.envar.getEnvVarValue
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.TracerProviderConfigModel
import io.opentelemetry.kotlin.config.model.resolve
import io.opentelemetry.kotlin.config.resolveOpenTelemetryConfig
import io.opentelemetry.kotlin.error.GuardedSdkErrorHandler
import io.opentelemetry.kotlin.error.NoopSdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.factory.IdGenerator
import io.opentelemetry.kotlin.factory.IdGeneratorImpl
import io.opentelemetry.kotlin.factory.ResourceFactory
import io.opentelemetry.kotlin.factory.ResourceFactoryImpl
import io.opentelemetry.kotlin.propagation.TextMapPropagator
import io.opentelemetry.kotlin.resource.detectResource
import kotlin.concurrent.Volatile

internal class OpenTelemetryConfigImpl(
    clock: Clock,
    private val resourceFactory: ResourceFactory = ResourceFactoryImpl(),
    private val globalResourceConfig: ResourceConfigImpl = ResourceConfigImpl(),
    private val readEnvVar: (String) -> String? = ::getEnvVarValue,
) : OpenTelemetryConfigDsl, ResourceConfigDsl by globalResourceConfig {

    @Volatile private var configuredErrorHandler: SdkErrorHandler = NoopSdkErrorHandler

    /**
     * The handler is configured after the sub-configs below have been created, so they receive a
     * forwarder that resolves the configured handler on each report instead.
     */
    private val sdkErrorHandler = GuardedSdkErrorHandler { configuredErrorHandler.onError(it) }

    internal val tracingConfig: TracerProviderConfigImpl = TracerProviderConfigImpl(clock, sdkErrorHandler)
    internal val loggingConfig: LoggerProviderConfigImpl = LoggerProviderConfigImpl(clock, sdkErrorHandler)
    internal val metricsConfig: MeterProviderConfigImpl = MeterProviderConfigImpl(sdkErrorHandler)
    internal val contextConfig: ContextConfigImpl = ContextConfigImpl()
    internal val propagatorCfg: PropagatorConfigImpl = PropagatorConfigImpl()
    private val globalAttributeLimits = AttributeLimitsConfigModelBuilder()
    private val resourceDetectionConfig = ResourceDetectionConfigImpl()

    private var customIdGenerator: (() -> IdGenerator)? = null

    override fun attributeLimits(action: AttributeLimitsConfigDsl.() -> Unit) {
        globalAttributeLimits.action()
    }

    override fun tracerProvider(action: TracerProviderConfigDsl.() -> Unit) {
        tracingConfig.action()
    }

    override fun loggerProvider(action: LoggerProviderConfigDsl.() -> Unit) {
        loggingConfig.action()
    }

    override fun meterProvider(action: MeterProviderConfigDsl.() -> Unit) {
        metricsConfig.action()
    }

    override fun resourceDetection(action: ResourceDetectionConfigDsl.() -> Unit) {
        resourceDetectionConfig.action()
    }

    override fun context(action: ContextConfigDsl.() -> Unit) {
        contextConfig.action()
    }

    override fun propagator(action: PropagatorConfigDsl.() -> TextMapPropagator) {
        propagatorCfg.action()
    }

    override fun idGenerator(action: () -> IdGenerator) {
        customIdGenerator = action
    }

    override fun errorHandler(handler: SdkErrorHandler) {
        configuredErrorHandler = handler
    }

    internal fun resolveIdGenerator(): IdGenerator = customIdGenerator?.invoke() ?: IdGeneratorImpl()

    private val baseResource by lazy {
        sdkDefaultResource()
            .merge(resourceDetectionConfig.detectors.detectResource(resourceFactory, sdkErrorHandler))
            .merge(globalResourceConfig.generateResource())
    }

    /**
     * The configuration every mechanism agreed on, resolved once so that each signal below reads
     * the same answer.
     *
     * The declarative configuration file is not located or read yet, so only the DSL and the
     * environment contribute today.
     */
    private val resolvedConfig by lazy {
        resolveOpenTelemetryConfig(
            dsl = OpenTelemetryConfigModel(
                tracerProvider = TracerProviderConfigModel(
                    spanLimits = tracingConfig.spanLimitsModel(globalAttributeLimits),
                ),
            ),
            readEnvVar = readEnvVar,
        )
    }

    internal fun generateTracingConfig() = tracingConfig.generateTracingConfig(
        base = baseResource,
        spanLimits = resolvedConfig.tracerProvider?.spanLimits.resolve(),
    )

    internal fun generateLoggingConfig() =
        loggingConfig.generateLoggingConfig(baseResource, globalAttributeLimits)

    internal fun generateMetricsConfig() =
        metricsConfig.generateMetricsConfig(baseResource)
}
