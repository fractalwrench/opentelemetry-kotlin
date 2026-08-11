package io.opentelemetry.kotlin.config.envar

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.envar.tracing.SpanLimitEnvVarConfigProcessor
import io.opentelemetry.kotlin.config.envar.tracing.SpanLimitEnvVarConfigProcessorImpl
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.config.model.TracerProviderConfigModel

/**
 * The configuration the process environment declares, as one layer for the config resolver.
 *
 * https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/
 */
@ExperimentalApi
class EnvVarConfigSource(
    private val readEnvVar: (String) -> String? = ::getEnvVarValue,
    private val spanLimitProcessor: SpanLimitEnvVarConfigProcessor =
        SpanLimitEnvVarConfigProcessorImpl(EnvVarConstants.SpanLimits.envVars),
) {

    /**
     * Returns what the environment declared, or `null` if it declared nothing this SDK reads.
     */
    fun resolve(): OpenTelemetryConfigModel? {
        val spanLimits = spanLimitProcessor
            .configure(SpanLimitsConfigModel()) { envVar -> readEnvVar(envVar) }
            .takeIf { it != SpanLimitsConfigModel() }
            ?: return null

        return OpenTelemetryConfigModel(
            tracerProvider = TracerProviderConfigModel(spanLimits = spanLimits),
        )
    }
}
