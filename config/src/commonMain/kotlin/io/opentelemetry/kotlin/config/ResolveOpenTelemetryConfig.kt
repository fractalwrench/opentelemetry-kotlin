package io.opentelemetry.kotlin.config

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.envar.EnvVarConfigSource
import io.opentelemetry.kotlin.config.envar.getEnvVarValue
import io.opentelemetry.kotlin.config.model.ConfigResolverImpl
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel

/**
 * Gathers the configuration each mechanism supplied and resolves it into the single model an SDK
 * is initialized with.
 *
 * Precedence is `SDK defaults < (envars or declarative config file) < DSL`, as described on
 * [io.opentelemetry.kotlin.config.model.ConfigResolver].
 *
 * The declarative configuration file is not located or read yet: pass an already-parsed document
 * mapped through [io.opentelemetry.kotlin.config.yaml.toConfigModel] as [declarativeFile] to
 * supply one. Everything downstream of that already honours it.
 *
 * https://opentelemetry.io/docs/specs/otel/configuration/
 */
@ExperimentalApi
fun resolveOpenTelemetryConfig(
    dsl: OpenTelemetryConfigModel? = null,
    declarativeFile: OpenTelemetryConfigModel? = null,
    readEnvVar: (String) -> String? = ::getEnvVarValue,
): OpenTelemetryConfigModel = ConfigResolverImpl().resolve(
    envars = EnvVarConfigSource(readEnvVar).resolve(),
    declarativeFile = declarativeFile,
    dsl = dsl,
)
