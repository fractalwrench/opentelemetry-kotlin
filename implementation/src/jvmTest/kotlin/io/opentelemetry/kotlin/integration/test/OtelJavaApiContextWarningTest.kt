package io.opentelemetry.kotlin.integration.test

import io.opentelemetry.kotlin.context.createOtelJavaCurrentContextStorage
import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.error.FakeSdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorSeverity
import io.opentelemetry.kotlin.framework.SdkTracker
import io.opentelemetry.kotlin.toOtelJavaApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class OtelJavaApiContextWarningTest {

    private val sdks = SdkTracker()

    @AfterTest
    fun tearDown() = sdks.shutdownAll()

    @Test
    fun testWarnsOncePerInstanceWithoutOptIn() {
        val errorHandler = FakeSdkErrorHandler()
        val otel = createOpenTelemetry { errorHandler(errorHandler) }.also(sdks::track)
        otel.toOtelJavaApi()
        otel.toOtelJavaApi()

        val error = errorHandler.apiMisuses.single()
        assertEquals("toOtelJavaApi", error.api)
        assertEquals(SdkErrorSeverity.WARNING, error.severity)
    }

    @Test
    fun testWarnsOnlyForTheInstanceThatInstalledTheDefaultStorage() {
        val errorHandler = FakeSdkErrorHandler()
        createOpenTelemetry { errorHandler(errorHandler) }.also(sdks::track).toOtelJavaApi()

        // the second SDK shares the first one's storage rather than installing its own
        createOpenTelemetry { errorHandler(errorHandler) }.also(sdks::track).toOtelJavaApi()
        assertEquals(1, errorHandler.apiMisuses.size)
    }

    @Test
    fun testNoWarningWithOptIn() {
        val errorHandler = FakeSdkErrorHandler()
        createOpenTelemetry {
            errorHandler(errorHandler)
            context { storage = createOtelJavaCurrentContextStorage() }
        }.also(sdks::track).toOtelJavaApi()
        assertTrue(errorHandler.apiMisuses.isEmpty())
    }
}
