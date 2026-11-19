package io.opentelemetry.kotlin.docs.snippets.readme

import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.tracing.export.batchSpanProcessor
import io.opentelemetry.kotlin.tracing.export.otlpHttpSpanExporter

// region readme-quickstart
suspend fun main() {
    val otel = createOpenTelemetry {
        serviceName = "my-service"
        tracerProvider {
            export {
                batchSpanProcessor(
                    otlpHttpSpanExporter {
                        endpoint = "http://localhost:4318" // override with your collector's URL
                    }
                )
            }
        }
    }
    otel.tracerProvider.getTracer("com.example").startSpan("hello").end()
    otel.shutdown() // flushes buffered spans before stopping
}
// end-region
