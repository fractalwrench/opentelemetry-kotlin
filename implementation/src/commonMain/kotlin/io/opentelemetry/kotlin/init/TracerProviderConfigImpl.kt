package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.Clock
import io.opentelemetry.kotlin.config.dsl.AttributeLimitsConfigModelBuilder
import io.opentelemetry.kotlin.config.dsl.buildSpanLimitsModel
import io.opentelemetry.kotlin.config.model.SpanLimits
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.factory.SpanFactory
import io.opentelemetry.kotlin.init.config.TracingConfig
import io.opentelemetry.kotlin.platformLog
import io.opentelemetry.kotlin.resource.Resource
import io.opentelemetry.kotlin.tracing.TracerConfigImpl
import io.opentelemetry.kotlin.tracing.TracerConfigurator
import io.opentelemetry.kotlin.tracing.export.SpanProcessor
import io.opentelemetry.kotlin.tracing.sampling.Sampler
import io.opentelemetry.kotlin.tracing.sampling.alwaysOn
import io.opentelemetry.kotlin.tracing.sampling.parentBased

internal class TracerProviderConfigImpl(
    private val clock: Clock,
    private val sdkErrorHandler: SdkErrorHandler,
    private val resourceConfigImpl: ResourceConfigImpl = ResourceConfigImpl()
) : TracerProviderConfigDsl, ResourceConfigDsl by resourceConfigImpl {

    private var processor: SpanProcessor? = null
    private var spanLimitsAction: SpanLimitsConfigDsl.() -> Unit = {}
    private var samplerAction: SamplerConfigDsl.() -> Sampler = { parentBased(root = alwaysOn()) }
    private val defaultTracerConfig = TracerConfigImpl(true)
    private var tracerConfigurator: TracerConfigurator = TracerConfigurator {
        defaultTracerConfig
    }

    override fun spanLimits(action: SpanLimitsConfigDsl.() -> Unit) {
        spanLimitsAction = action
    }

    override fun export(action: TraceExportConfigDsl.() -> SpanProcessor) {
        if (processor != null) {
            platformLog("export() should only be called once.")
            return
        }
        processor = TraceExportConfigImpl(clock, sdkErrorHandler).action()
    }

    override fun sampler(action: SamplerConfigDsl.() -> Sampler) {
        samplerAction = action
    }

    override fun tracerConfigurator(configurator: TracerConfigurator) {
        tracerConfigurator = configurator
    }

    /**
     * The span limits the DSL declared, as one layer for the config resolver. Limits the DSL did
     * not mention are left unset so that a lower-precedence mechanism can supply them.
     */
    fun spanLimitsModel(globalLimits: AttributeLimitsConfigModelBuilder? = null): SpanLimitsConfigModel? =
        buildSpanLimitsModel(globalLimits, spanLimitsAction)

    /**
     * @param spanLimits the limits resolved from every configuration mechanism, not just the DSL.
     */
    fun generateTracingConfig(base: Resource, spanLimits: SpanLimits): TracingConfig = TracingConfig(
        processor = processor,
        spanLimits = spanLimits,
        resource = base.merge(resourceConfigImpl.generateResource()),
        sdkErrorHandler = sdkErrorHandler,
        samplerFactory = { spanFactory -> SamplerConfigImpl(spanFactory).samplerAction() },
        tracerConfigurator = tracerConfigurator,
    )

    private class SamplerConfigImpl(override val spanFactory: SpanFactory) : SamplerConfigDsl
}
