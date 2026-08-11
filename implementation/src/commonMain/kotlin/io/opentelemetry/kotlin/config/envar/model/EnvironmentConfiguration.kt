package io.opentelemetry.kotlin.config.envar.model

import io.opentelemetry.kotlin.init.config.LogLimitConfig

/**
 * Span limits are no longer read here: they are supplied to the config resolver by
 * [io.opentelemetry.kotlin.config.envar.EnvVarConfigSource] alongside the other configuration
 * mechanisms. Log limits are still to be moved onto that model.
 */
internal data class EnvironmentConfiguration(
    val logLimitConfig: LogLimitConfig,
)
