package io.opentelemetry.kotlin.tracing.model

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.attributes.ReadableAttributes

/**
 * A writable representation of a [Span] that can be modified.
 */
@ExperimentalApi
public interface ReadWriteSpan : Span, ReadableSpan, ReadableAttributes
