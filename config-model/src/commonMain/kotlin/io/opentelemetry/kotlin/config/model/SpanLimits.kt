package io.opentelemetry.kotlin.config.model

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.ThreadSafe

/**
 * The span limits the SDK actually applies, once every configuration mechanism has had its say.
 *
 * Unlike [SpanLimitsConfigModel], which leaves a limit `null` when no mechanism configured it,
 * every limit here has a value: either the one that was configured, or the default the
 * specification prescribes.
 *
 * https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-limits
 */
@ExperimentalApi
@ThreadSafe
data class SpanLimits(

    /**
     * Maximum number of attributes that may be recorded on a span.
     */
    val attributeCountLimit: Int = DEFAULT_ATTRIBUTE_COUNT_LIMIT,

    /**
     * Maximum length of a recorded attribute value.
     */
    val attributeValueLengthLimit: Int = DEFAULT_ATTRIBUTE_VALUE_LENGTH_LIMIT,

    /**
     * Maximum number of links that may be recorded on a span.
     */
    val linkCountLimit: Int = DEFAULT_LINK_COUNT_LIMIT,

    /**
     * Maximum number of events that may be recorded on a span.
     */
    val eventCountLimit: Int = DEFAULT_EVENT_COUNT_LIMIT,

    /**
     * Maximum number of attributes that may be recorded on a single event.
     */
    val attributeCountPerEventLimit: Int = DEFAULT_ATTRIBUTE_COUNT_LIMIT,

    /**
     * Maximum number of attributes that may be recorded on a single link.
     */
    val attributeCountPerLinkLimit: Int = DEFAULT_ATTRIBUTE_COUNT_LIMIT,
)

/**
 * Applies the specification defaults to every limit this model left unset.
 *
 * A `null` receiver means no mechanism configured any limit, so every default applies.
 */
@ExperimentalApi
fun SpanLimitsConfigModel?.resolve(): SpanLimits = SpanLimits(
    attributeCountLimit = this?.attributeCountLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT,
    attributeValueLengthLimit = this?.attributeValueLengthLimit ?: DEFAULT_ATTRIBUTE_VALUE_LENGTH_LIMIT,
    linkCountLimit = this?.linkCountLimit ?: DEFAULT_LINK_COUNT_LIMIT,
    eventCountLimit = this?.eventCountLimit ?: DEFAULT_EVENT_COUNT_LIMIT,
    attributeCountPerEventLimit = this?.attributeCountPerEventLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT,
    attributeCountPerLinkLimit = this?.attributeCountPerLinkLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT,
)

/**
 * https://opentelemetry.io/docs/specs/otel/common/#attribute-limits
 */
@ExperimentalApi
const val DEFAULT_ATTRIBUTE_COUNT_LIMIT: Int = 128

/**
 * The specification prescribes no limit on attribute value length by default.
 */
@ExperimentalApi
const val DEFAULT_ATTRIBUTE_VALUE_LENGTH_LIMIT: Int = Int.MAX_VALUE

/**
 * https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-limits
 */
@ExperimentalApi
const val DEFAULT_LINK_COUNT_LIMIT: Int = 128

/**
 * https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-limits
 */
@ExperimentalApi
const val DEFAULT_EVENT_COUNT_LIMIT: Int = 128
