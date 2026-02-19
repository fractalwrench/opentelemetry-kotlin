package io.opentelemetry.kotlin.tracing.data

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.ThreadSafe
import io.opentelemetry.kotlin.attributes.ReadableAttributes
import io.opentelemetry.kotlin.tracing.model.SpanContext

/**
 * A read-only representation of a Link
 */
@ExperimentalApi
public interface LinkData : ReadableAttributes {

    /**
     * The span context of the link.
     */
    @ThreadSafe
    public val spanContext: SpanContext
}
