package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaContext
import io.opentelemetry.kotlin.aliases.OtelJavaContextStorage

/**
 * Returns a new [CurrentContextStorage] backed by opentelemetry-java's `ContextStorage`, so that
 * Kotlin and Java code share one current context: a Kotlin `attach()` is visible through Java's
 * `Span.current()`, and a Java `makeCurrent()` is visible through [currentContext].
 *
 * Compat mode installs this automatically. Pass it to the SDK's `context { storage = ... }` when
 * you use the Kotlin SDK alongside opentelemetry-java, for example via `toOtelJavaApi()`.
 *
 * Every call returns a new instance, but all instances share Java's process-wide storage.
 */
@ExperimentalApi
public fun createOtelJavaCurrentContextStorage(): CurrentContextStorage = OtelJavaCurrentContextStorage()

@OptIn(ExperimentalApi::class)
internal class OtelJavaCurrentContextStorage : CurrentContextStorage {

    override fun current(): Context = OtelJavaContext.current().toOtelKotlinContext()

    override fun attach(context: Context): Scope {
        val javaContext = OtelJavaContextBridge.toOtelJava(context, OtelJavaContext.current())
        return ScopeAdapter(OtelJavaContextStorage.get().attach(javaContext))
    }
}
