package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaContext

/**
 * Converts this Kotlin [Context] to an opentelemetry-java context, for passing as an explicit
 * parent to Java APIs. Its span and baggage are visible to Java code, and every Kotlin value
 * survives a round trip through [toOtelKotlinContext].
 */
@ExperimentalApi
public fun Context.toOtelJavaContext(): OtelJavaContext = when {
    this === rootContext() -> OtelJavaContext.root()
    else -> OtelJavaContextBridge.toOtelJava(this, OtelJavaContext.root())
}
