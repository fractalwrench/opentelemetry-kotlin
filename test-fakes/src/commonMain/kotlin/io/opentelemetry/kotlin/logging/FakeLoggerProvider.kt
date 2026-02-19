package io.opentelemetry.kotlin.logging

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.attributes.WritableAttributes

@OptIn(ExperimentalApi::class)
class FakeLoggerProvider : LoggerProvider {

    val map = mutableMapOf<String, FakeLogger>()

    override fun getLogger(
        name: String,
        version: String?,
        schemaUrl: String?,
        attributes: (WritableAttributes.() -> Unit)?
    ): Logger = map.getOrPut(name) {
        FakeLogger(name)
    }
}
