package io.opentelemetry.kotlin.propagation

/**
 * Marks declarations that exist so that SDK initialization can wire up propagation. They are not
 * part of the API that applications or instrumentation should use, and may change at any time.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalPropagatorApi
