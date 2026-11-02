package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaContext

/**
 * Converts this opentelemetry-java context to a Kotlin [Context]. A context that came from
 * [toOtelJavaContext] returns the original Kotlin values; a span or baggage that Java code
 * changed since is overlaid on top.
 */
@ExperimentalApi
public fun OtelJavaContext.toOtelKotlinContext(): Context = OtelJavaContextBridge.toOtelKotlin(this)
