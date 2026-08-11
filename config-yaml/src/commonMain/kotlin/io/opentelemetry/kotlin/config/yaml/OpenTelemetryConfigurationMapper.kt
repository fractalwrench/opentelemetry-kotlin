package io.opentelemetry.kotlin.config.yaml

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.config.model.TracerProviderConfigModel
import io.opentelemetry.kotlin.config.model.mergeNode
import io.opentelemetry.kotlin.config.schema.model.AttributeLimits
import io.opentelemetry.kotlin.config.schema.model.OpenTelemetryConfiguration
import io.opentelemetry.kotlin.config.schema.model.SpanLimits

/**
 * Maps a parsed declarative-configuration document onto the model the SDK resolves configuration
 * with, so the file can take part in precedence alongside the DSL and the environment.
 *
 * Only the parts of the schema this SDK understands are carried across; everything else is left
 * unset, which means "no mechanism configured this".
 *
 * https://opentelemetry.io/docs/specs/otel/configuration/data-model/
 */
@ExperimentalApi
fun OpenTelemetryConfiguration.toConfigModel(): OpenTelemetryConfigModel =
    OpenTelemetryConfigModel(
        tracerProvider = toTracerProviderConfigModel(),
    )

private fun OpenTelemetryConfiguration.toTracerProviderConfigModel(): TracerProviderConfigModel? {
    val spanLimits = mergeNode(attributeLimits?.toSpanLimitsConfigModel(), tracerProvider?.limits?.toConfigModel())
        ?: return null
    return TracerProviderConfigModel(spanLimits = spanLimits)
}

/**
 * The general attribute limits are the weaker statement: `tracer_provider.limits` overrides them
 * where the two overlap, and they apply to spans wherever it does not.
 */
private fun AttributeLimits.toSpanLimitsConfigModel(): SpanLimitsConfigModel? =
    SpanLimitsConfigModel(
        attributeCountLimit = attributeCountLimit.toLimitOrNull(),
        attributeValueLengthLimit = attributeValueLengthLimit.toLimitOrNull(),
    ).takeIf { it != SpanLimitsConfigModel() }

private fun SpanLimits.toConfigModel(): SpanLimitsConfigModel? =
    SpanLimitsConfigModel(
        attributeCountLimit = attributeCountLimit.toLimitOrNull(),
        attributeValueLengthLimit = attributeValueLengthLimit.toLimitOrNull(),
        linkCountLimit = linkCountLimit.toLimitOrNull(),
        eventCountLimit = eventCountLimit.toLimitOrNull(),
        attributeCountPerEventLimit = eventAttributeCountLimit.toLimitOrNull(),
        attributeCountPerLinkLimit = linkAttributeCountLimit.toLimitOrNull(),
    ).takeIf { it != SpanLimitsConfigModel() }

/**
 * The schema types limits as `Long`, but the SDK applies them as `Int`. A value the SDK cannot
 * represent, or one the schema forbids by being negative, is treated as though it were absent so
 * that a lower-precedence mechanism can still supply the limit.
 */
private fun Long?.toLimitOrNull(): Int? = this?.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
