package io.opentelemetry.kotlin.tracing

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.attributes.CompatAttributesModel
import io.opentelemetry.kotlin.attributes.ReadableAttributes
import io.opentelemetry.kotlin.attributes.WritableAttributes
import io.opentelemetry.kotlin.tracing.model.SpanEvent

@OptIn(ExperimentalApi::class)
internal class SpanEventCompatImpl(
    override val name: String,
    override val timestamp: Long,
    private val attrs: CompatAttributesModel
) : SpanEvent, WritableAttributes by attrs, ReadableAttributes by attrs
