package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaSpanLimits
import io.opentelemetry.kotlin.config.model.SpanLimits

/**
 * Hands the resolved span limits to the Java SDK, which enforces them on the spans it exports.
 */
@ExperimentalApi
internal fun SpanLimits.toOtelJavaSpanLimits(): OtelJavaSpanLimits = OtelJavaSpanLimits.builder()
    .setMaxNumberOfAttributes(attributeCountLimit)
    .setMaxAttributeValueLength(attributeValueLengthLimit)
    .setMaxNumberOfLinks(linkCountLimit)
    .setMaxNumberOfEvents(eventCountLimit)
    .setMaxNumberOfAttributesPerEvent(attributeCountPerEventLimit)
    .setMaxNumberOfAttributesPerLink(attributeCountPerLinkLimit)
    .build()
