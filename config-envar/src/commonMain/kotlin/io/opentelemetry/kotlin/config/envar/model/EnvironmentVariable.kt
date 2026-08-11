package io.opentelemetry.kotlin.config.envar.model

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * One environment variable the SDK looked up, and the value it parsed from it.
 *
 * A `null` [value] means the variable was unset, or held something that could not be parsed.
 */
@ExperimentalApi
data class EnvironmentVariable<T>(val name: EnvVarName, val value: T?)
