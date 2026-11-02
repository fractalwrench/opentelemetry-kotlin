@file:Suppress("DiscouragedImport")

package io.opentelemetry.kotlin.context

import io.opentelemetry.api.baggage.otelJavaBaggageContextKey
import io.opentelemetry.api.trace.otelJavaSpanContextKey
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaBaggage
import io.opentelemetry.kotlin.aliases.OtelJavaContext
import io.opentelemetry.kotlin.aliases.OtelJavaContextKey
import io.opentelemetry.kotlin.aliases.OtelJavaSpan
import io.opentelemetry.kotlin.baggage.toOtelJavaBaggage
import io.opentelemetry.kotlin.baggage.toOtelKotlinBaggage
import io.opentelemetry.kotlin.tracing.Span
import io.opentelemetry.kotlin.tracing.createInvalidSpanContext
import io.opentelemetry.kotlin.tracing.createNonRecordingSpan
import io.opentelemetry.kotlin.tracing.ext.toOtelJavaSpan
import io.opentelemetry.kotlin.tracing.ext.toOtelKotlinSpanContext
import io.opentelemetry.kotlin.tracing.model.OtelJavaSpanAdapter
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * Converts between Kotlin [Context] values and opentelemetry-java contexts.
 *
 * Java's Context can't be iterated, and Java instrumentation reads Java's own span and baggage
 * keys. So a Kotlin context is carried through Java as:
 * - the whole Kotlin context, under one private Java key, so custom Kotlin keys survive;
 * - its span and baggage, mirrored into Java's keys, so Java code sees them;
 * - the mirrored span and baggage again, under private keys, so that changes made by Java code
 *   (e.g. `javaSpan.makeCurrent()`) can be detected and overlaid on the way back.
 *
 * A Java context that this bridge didn't produce (Java code built or changed it) converts to a
 * Kotlin context that remembers it as its origin. Converting that Kotlin context back to Java
 * starts from the origin, so Java-only keys survive a Kotlin round trip.
 */
@OptIn(ExperimentalApi::class)
internal object OtelJavaContextBridge {

    private val KOTLIN_CONTEXT: OtelJavaContextKey<Context> = OtelJavaContextKey.named("otel-kotlin-context")
    private val MIRRORED_SPAN: OtelJavaContextKey<OtelJavaSpan> = OtelJavaContextKey.named("otel-kotlin-span")
    private val MIRRORED_BAGGAGE: OtelJavaContextKey<OtelJavaBaggage> =
        OtelJavaContextKey.named("otel-kotlin-baggage")
    private val JAVA_ORIGIN: ContextKey<OtelJavaContext> = createContextKey("otel-java-origin")

    /**
     * Java contexts produced by [toOtelJava]. Converting one back returns the Kotlin context it was
     * produced from, unchanged.
     */
    private val produced: MutableMap<OtelJavaContext, Boolean> = Collections.synchronizedMap(WeakHashMap())

    /**
     * Kotlin contexts converted from Java contexts this bridge didn't produce. Java contexts compare
     * by identity, so caching keeps `current()` returning the same instance for the same Java
     * context, which the API's scope-misuse checks rely on. Values are weak because each one
     * references its key as its origin.
     */
    private val converted: MutableMap<OtelJavaContext, WeakReference<Context>> =
        Collections.synchronizedMap(WeakHashMap())

    fun toOtelJava(context: Context, base: OtelJavaContext): OtelJavaContext {
        val origin = context.get(JAVA_ORIGIN)
        if (origin != null && converted[origin]?.get() === context) {
            // converted from Java and not changed since, so hand back the original Java context
            return origin
        }
        val span = context.extractSpan()
        val javaSpan = when {
            span.spanContext.isValid -> span.toOtelJavaSpan()
            else -> OtelJavaSpan.getInvalid()
        }
        val javaBaggage = context.extractBaggage().toOtelJavaBaggage()
        val result = (origin ?: base)
            .with(KOTLIN_CONTEXT, context)
            .with(otelJavaSpanContextKey, javaSpan)
            .with(MIRRORED_SPAN, javaSpan)
            .with(otelJavaBaggageContextKey, javaBaggage)
            .with(MIRRORED_BAGGAGE, javaBaggage)
        produced[result] = true
        return result
    }

    fun toOtelKotlin(context: OtelJavaContext): Context {
        if (context === OtelJavaContext.root()) {
            return rootContext()
        }
        val stored = context.get(KOTLIN_CONTEXT)
        if (stored != null && produced.containsKey(context)) {
            return stored
        }
        converted[context]?.get()?.let { return it }

        var result = stored ?: rootContext()
        val javaSpan = OtelJavaSpan.fromContextOrNull(context)
        if (javaSpan !== context.get(MIRRORED_SPAN)) {
            result = result.storeSpan(javaSpan.toOtelKotlinSpan())
        }
        val javaBaggage = OtelJavaBaggage.fromContextOrNull(context)
        if (javaBaggage !== context.get(MIRRORED_BAGGAGE)) {
            result = when (javaBaggage) {
                null -> result.clearBaggage()
                else -> result.storeBaggage(javaBaggage.toOtelKotlinBaggage())
            }
        }
        result = result.set(JAVA_ORIGIN, context)
        converted[context] = WeakReference(result)
        return result
    }

    private fun OtelJavaSpan?.toOtelKotlinSpan(): Span = when (this) {
        is OtelJavaSpanAdapter -> span
        null -> createNonRecordingSpan(createInvalidSpanContext())
        else -> createNonRecordingSpan(spanContext.toOtelKotlinSpanContext())
    }
}
