package io.opentelemetry.kotlin.tracing.export

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.FakeInstrumentationScopeInfo
import io.opentelemetry.kotlin.export.OperationResultCode
import io.opentelemetry.kotlin.framework.loadTestFixture
import io.opentelemetry.kotlin.resource.FakeResource
import io.opentelemetry.kotlin.tracing.FakeReadWriteSpan
import io.opentelemetry.kotlin.tracing.FakeSpanContext
import io.opentelemetry.kotlin.tracing.data.FakeEventData
import io.opentelemetry.kotlin.tracing.data.StatusData
import io.opentelemetry.kotlin.tracing.model.SpanKind
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalApi::class)
internal class StdoutSpanExporterTest {

    @Test
    fun testExportSpan() {
        val output = mutableListOf<String>()
        val exporter = createStdoutSpanExporter(logger = { output.add(it) })

        val span = FakeReadWriteSpan(
            name = "test-span",
            spanKind = SpanKind.SERVER,
            status = StatusData.Ok,
            spanContext = FakeSpanContext.VALID,
            parent = FakeSpanContext.INVALID,
            startTimestamp = 1000000000L,
            endTimestamp = 2000000000L,
            attributes = mapOf("http.method" to "GET", "http.status_code" to 200),
            events = listOf(
                FakeEventData(name = "request.started", timestamp = 1100000000L),
                FakeEventData(name = "request.completed", timestamp = 1900000000L)
            ),
            links = emptyList(),
            resource = FakeResource(attributes = mapOf("service.name" to "test-service")),
            instrumentationScopeInfo = FakeInstrumentationScopeInfo(
                name = "io.opentelemetry.test",
                version = "1.0.0"
            ),
            hasEnded = true
        )

        val result = exporter.export(listOf(span))
        assertEquals(OperationResultCode.Success, result)
        assertEquals(1, output.size)

        val expected = loadTestFixture("stdout_span_output.txt")
        assertEquals(expected, output.single())
    }

    @Test
    fun testForceFlush() {
        val exporter = StdoutSpanExporter()
        assertEquals(OperationResultCode.Success, exporter.forceFlush())
    }

    @Test
    fun testShutdown() {
        val exporter = StdoutSpanExporter()
        assertEquals(OperationResultCode.Success, exporter.shutdown())
    }
}
