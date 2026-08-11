package io.opentelemetry.kotlin.tracing

import io.opentelemetry.kotlin.config.model.SpanLimits
import io.opentelemetry.kotlin.init.config.LogLimitConfig

internal val fakeSpanLimitsConfig = SpanLimits(
    attributeCountLimit = 100,
    attributeValueLengthLimit = Int.MAX_VALUE,
    linkCountLimit = 100,
    eventCountLimit = 100,
    attributeCountPerEventLimit = 100,
    attributeCountPerLinkLimit = 100
)

internal val fakeLogLimitsConfig = LogLimitConfig(
    attributeCountLimit = 100,
    attributeValueLengthLimit = 100,
)
