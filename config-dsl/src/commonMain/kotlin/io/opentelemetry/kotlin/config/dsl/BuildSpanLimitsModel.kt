package io.opentelemetry.kotlin.config.dsl

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.config.model.mergeNode
import io.opentelemetry.kotlin.init.SpanLimitsConfigDsl

/**
 * Folds the two places the DSL can declare span limits into the single node the config model uses.
 *
 * [globalAttributeLimits] come from `attributeLimits { }`, which applies to every signal, and
 * [spanLimits] comes from `tracerProvider { spanLimits { } }`, which is span-specific and so wins
 * where the two overlap. Returns `null` when neither declared anything, so that the DSL
 * contributes an empty layer rather than one full of defaults.
 *
 * https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-limits
 */
@ExperimentalApi
fun buildSpanLimitsModel(
    globalAttributeLimits: AttributeLimitsConfigModelBuilder? = null,
    spanLimits: (SpanLimitsConfigDsl.() -> Unit)? = null,
): SpanLimitsConfigModel? {
    val global = globalAttributeLimits?.let { limits ->
        SpanLimitsConfigModel(
            attributeCountLimit = limits.configuredAttributeCountLimit,
            attributeValueLengthLimit = limits.configuredAttributeValueLengthLimit,
        ).takeIf { it != SpanLimitsConfigModel() }
    }
    val specific = SpanLimitsConfigModelBuilder().apply { spanLimits?.invoke(this) }.toModel()
    return mergeNode(global, specific)
}
