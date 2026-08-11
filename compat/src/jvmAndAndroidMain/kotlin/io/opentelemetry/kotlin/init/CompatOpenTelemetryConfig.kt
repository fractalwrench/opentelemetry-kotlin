package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.Clock
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaResource
import io.opentelemetry.kotlin.attributes.AttributesMutator
import io.opentelemetry.kotlin.attributes.CompatAttributesModel
import io.opentelemetry.kotlin.attributes.setTypedAttributes
import io.opentelemetry.kotlin.config.dsl.AttributeLimitsConfigModelBuilder
import io.opentelemetry.kotlin.config.envar.getEnvVarValue
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.SpanLimits
import io.opentelemetry.kotlin.config.model.TracerProviderConfigModel
import io.opentelemetry.kotlin.config.model.resolve
import io.opentelemetry.kotlin.config.resolveOpenTelemetryConfig
import io.opentelemetry.kotlin.error.GuardedSdkErrorHandler
import io.opentelemetry.kotlin.error.NoopSdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.factory.CompatIdGenerator
import io.opentelemetry.kotlin.factory.CompatResourceFactory
import io.opentelemetry.kotlin.factory.IdGenerator
import io.opentelemetry.kotlin.propagation.CompatPropagatorConfigImpl
import io.opentelemetry.kotlin.propagation.TextMapPropagator
import io.opentelemetry.kotlin.resource.Resource
import io.opentelemetry.kotlin.resource.ResourceAdapter
import io.opentelemetry.kotlin.resource.detectResource
import io.opentelemetry.kotlin.semconv.ServiceAttributes
import kotlin.concurrent.Volatile

@ExperimentalApi
internal class CompatOpenTelemetryConfig(
    clock: Clock,
    private val readEnvVar: (String) -> String? = ::getEnvVarValue,
) : OpenTelemetryConfigDsl {

    @Volatile private var configuredErrorHandler: SdkErrorHandler = NoopSdkErrorHandler
    private val sdkErrorHandler = GuardedSdkErrorHandler { configuredErrorHandler.onError(it) }

    internal val tracerProviderConfig = CompatTracerProviderConfig(clock, sdkErrorHandler)
    internal val loggerProviderConfig = CompatLoggerProviderConfig(clock, sdkErrorHandler)
    internal val meterProviderConfig = CompatMeterProviderConfig(clock)
    internal val globalAttributeLimits = AttributeLimitsConfigModelBuilder()
    internal val propagatorCfg = CompatPropagatorConfigImpl()

    private var customIdGenerator: (() -> IdGenerator)? = null

    override fun attributeLimits(action: AttributeLimitsConfigDsl.() -> Unit) {
        globalAttributeLimits.action()
    }

    private val globalResourceAttrs = CompatAttributesModel()
    private var globalResourceSchemaUrl: String? = null
    private var serviceNameOverride: String? = null

    override var serviceName: String
        get() = serviceNameOverride ?: "unknown_service"
        set(value) {
            serviceNameOverride = value
            globalResourceAttrs.setStringAttribute(ServiceAttributes.SERVICE_NAME, value)
        }

    override fun resource(schemaUrl: String?, attributes: AttributesMutator.() -> Unit) {
        globalResourceSchemaUrl = schemaUrl
        globalResourceAttrs.apply(attributes)
    }

    override fun resource(map: Map<String, Any>) {
        globalResourceAttrs.apply { setTypedAttributes(map) }
    }

    private val resourceDetectionConfig = CompatResourceDetectionConfig()

    override fun resourceDetection(action: ResourceDetectionConfigDsl.() -> Unit) {
        resourceDetectionConfig.action()
    }

    internal fun buildGlobalResource(): Resource {
        val declared =
            ResourceAdapter(OtelJavaResource.create(globalResourceAttrs.otelJavaAttributes(), globalResourceSchemaUrl))
        return resourceDetectionConfig.detectors.detectResource(CompatResourceFactory, sdkErrorHandler).merge(declared)
    }

    override fun context(action: ContextConfigDsl.() -> Unit) {
        // no-op
    }

    override fun tracerProvider(action: TracerProviderConfigDsl.() -> Unit) {
        tracerProviderConfig.action()
    }

    override fun loggerProvider(action: LoggerProviderConfigDsl.() -> Unit) {
        loggerProviderConfig.action()
    }

    override fun meterProvider(action: MeterProviderConfigDsl.() -> Unit) {
        meterProviderConfig.action()
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

    internal fun resolveIdGenerator(): IdGenerator = customIdGenerator?.invoke() ?: CompatIdGenerator()

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
                    spanLimits = tracerProviderConfig.spanLimitsModel(globalAttributeLimits),
                ),
            ),
            readEnvVar = readEnvVar,
        )
    }

    internal fun resolveSpanLimits(): SpanLimits = resolvedConfig.tracerProvider?.spanLimits.resolve()
}
