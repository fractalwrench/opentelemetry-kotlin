package io.opentelemetry.kotlin.logging

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.attributes.WritableAttributes

@ExperimentalApi
internal object NoopLoggerProvider : LoggerProvider {
    override fun getLogger(
        name: String,
        version: String?,
        schemaUrl: String?,
        attributes: (WritableAttributes.() -> Unit)?
    ): Logger = NoopLogger
}
