package io.opentelemetry.kotlin.factory

/**
 * Context is an API-level concern that stays functional when no SDK is installed, matching
 * opentelemetry-java, so that instrumentation propagating context doesn't silently drop it. Only
 * the telemetry a Context carries is noop: [NoopSpanFactory] means spans stored here are never
 * recorded.
 */
internal val NoopContextFactory: ContextFactory = ContextFactoryImpl(NoopSpanFactory)
