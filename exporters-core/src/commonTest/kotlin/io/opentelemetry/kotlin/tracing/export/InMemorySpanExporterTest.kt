package io.opentelemetry.kotlin.tracing.export

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.export.OperationResultCode.Success
import io.opentelemetry.kotlin.tracing.FakeReadWriteSpan
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalApi::class)
internal class InMemorySpanExporterTest {

    private val fakeTelemetry = listOf(FakeReadWriteSpan())
    private lateinit var exporter: InMemorySpanExporter

    @BeforeTest
    fun setUp() {
        exporter = createInMemorySpanExporter()
    }

    @Test
    fun testExporterShutdown() {
        assertEquals(Success, exporter.shutdown())
    }

    @Test
    fun testExporterForceFlush() {
        assertEquals(Success, exporter.forceFlush())
    }

    @Test
    fun testExport() {
        exporter.export(fakeTelemetry)
        assertEquals(fakeTelemetry, exporter.exportedSpans)
    }
}
