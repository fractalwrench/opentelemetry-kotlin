package io.opentelemetry.kotlin.context

/**
 * Marks functions that reset the process-wide [CurrentContextStorage]. They exist for test
 * isolation only.
 */
@RequiresOptIn(
    message = "Resets process-wide OpenTelemetry state. Only call this from tests.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class ProcessWideStateTestingApi
