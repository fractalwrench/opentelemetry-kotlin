package io.opentelemetry.kotlin

import io.opentelemetry.kotlin.error.FakeSdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorSeverity
import io.opentelemetry.kotlin.logging.export.FakeLogRecordProcessor
import io.opentelemetry.kotlin.tracing.export.FakeSpanProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A processor supplied to `export {}` without an explicit composite must still get the same fault
 * isolation as one that is composited, i.e. a throw from user code is reported to the error handler
 * rather than propagating into the instrumented application.
 */
@OptIn(ExperimentalApi::class)
internal class BareProcessorFaultIsolationTest {

    private val handler = FakeSdkErrorHandler()

    @Test
    fun `a bare span processor throwing in onStart does not reach the caller`() {
        val processor = FakeSpanProcessor(startAction = { _, _ -> error("boom") })
        val otel = otelWith(processor)

        otel.tracerProvider.getTracer("test").startSpan("span")

        assertEquals(1, processor.startCalls.size)
        assertSingleUserCodeError()
    }

    @Test
    fun `a bare span processor throwing in onEnding does not reach the caller and onEnd still fires`() {
        val processor = FakeSpanProcessor(endingAction = { error("boom") })
        val otel = otelWith(processor)

        val span = otel.tracerProvider.getTracer("test").startSpan("span")
        span.end()

        assertEquals(1, processor.endingCalls.size)
        assertEquals(1, processor.endCalls.size)
        assertFalse(span.isRecording())
        assertSingleUserCodeError()
    }

    @Test
    fun `a bare span processor throwing in onEnd does not reach the caller`() {
        val processor = FakeSpanProcessor(endAction = { error("boom") })
        val otel = otelWith(processor)

        otel.tracerProvider.getTracer("test").startSpan("span").end()

        assertEquals(1, processor.endCalls.size)
        assertSingleUserCodeError()
    }

    @Test
    fun `a bare span processor that requires neither callback receives neither`() {
        val processor = FakeSpanProcessor(startRequired = false, endRequired = false)
        val otel = otelWith(processor)

        otel.tracerProvider.getTracer("test").startSpan("span").end()

        assertTrue(processor.startCalls.isEmpty())
        assertTrue(processor.endingCalls.isEmpty())
        assertTrue(processor.endCalls.isEmpty())
        assertFalse(handler.hasErrors())
    }

    @Test
    fun `a bare log record processor throwing in onEmit does not reach the caller`() {
        val processor = FakeLogRecordProcessor(action = { _, _ -> error("boom") })
        val otel = createOpenTelemetry {
            errorHandler(handler)
            loggerProvider {
                export { processor }
            }
        }

        otel.loggerProvider.getLogger("test").emit(body = "log")

        assertEquals(1, processor.logs.size)
        assertSingleUserCodeError()
    }

    @Test
    fun `a bare log record processor throwing in enabled does not reach the caller`() {
        val processor = FakeLogRecordProcessor(enabledResult = { error("boom") })
        val otel = createOpenTelemetry {
            errorHandler(handler)
            loggerProvider {
                export { processor }
            }
        }

        assertFalse(otel.loggerProvider.getLogger("test").enabled())
        assertSingleUserCodeError()
    }

    private fun otelWith(processor: FakeSpanProcessor) = createOpenTelemetry {
        errorHandler(handler)
        tracerProvider {
            export { processor }
        }
    }

    private fun assertSingleUserCodeError() {
        val error = handler.userCodeErrors.single()
        assertEquals(SdkErrorSeverity.WARNING, error.severity)
        assertEquals("boom", error.cause.message)
    }
}
