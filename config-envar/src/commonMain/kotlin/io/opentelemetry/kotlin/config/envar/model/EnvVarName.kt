package io.opentelemetry.kotlin.config.envar.model

import io.opentelemetry.kotlin.ExperimentalApi
import kotlin.jvm.JvmInline

/**
 * The name of an environment variable the SDK reads configuration from.
 *
 * https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/
 */
@ExperimentalApi
@JvmInline
value class EnvVarName(val value: String) {
    init {
        require(value.startsWith("OTEL"))
    }

    companion object {
        fun envVarName(value: String) = EnvVarName(value)
    }
}
