package io.opentelemetry.kotlin.factory

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Baggage is an API-level concern that stays functional when no SDK is installed, matching
 * opentelemetry-java, so that instrumentation propagating baggage doesn't silently drop it.
 */
@OptIn(ExperimentalApi::class)
internal val NoopBaggageFactory: BaggageFactory = BaggageFactoryImpl()
