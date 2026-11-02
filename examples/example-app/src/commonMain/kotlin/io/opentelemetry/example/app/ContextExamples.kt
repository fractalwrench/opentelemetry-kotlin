@file:OptIn(ExperimentalApi::class)

package io.opentelemetry.example.app

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.baggage.createBaggage
import io.opentelemetry.kotlin.context.createContextKey
import io.opentelemetry.kotlin.context.currentContext
import io.opentelemetry.kotlin.context.rootContext
import io.opentelemetry.kotlin.context.runWithContext
import io.opentelemetry.kotlin.propagation.createCompositePropagator
import io.opentelemetry.kotlin.propagation.createMapTextMapGetter
import io.opentelemetry.kotlin.propagation.createMutableMapTextMapSetter
import io.opentelemetry.kotlin.propagation.createW3CBaggagePropagator
import io.opentelemetry.kotlin.propagation.createW3CTraceContextPropagator
import io.opentelemetry.kotlin.tracing.SpanKind
import io.opentelemetry.kotlin.tracing.Tracer

/**
 * A custom context key. Keys compare by identity, so keep one shared instance.
 */
private val REQUEST_ID = createContextKey<String>("request.id")

/**
 * Demonstrates the instance-free Context API: rootContext(), currentContext(), runWithContext,
 * custom keys and propagation. None of it needs the OpenTelemetry instance; the SDK installed the
 * process-wide CurrentContextStorage when it was created.
 */
internal fun demonstrateContextApi(tracer: Tracer) {
    val propagator = createCompositePropagator(
        createW3CTraceContextPropagator(),
        createW3CBaggagePropagator(),
    )

    val ctx = rootContext()
        .set(REQUEST_ID, "req-42")
        .storeBaggage(createBaggage { put("tenant", "acme") })

    ctx.runWithContext {
        println("Current request.id: ${currentContext().get(REQUEST_ID)}")

        val span = tracer.startSpan("outbound-call", spanKind = SpanKind.CLIENT)
        val headers = mutableMapOf<String, String>()
        currentContext().storeSpan(span).runWithContext {
            propagator.inject(currentContext(), headers, createMutableMapTextMapSetter())
        }
        span.end()
        println("Injected headers: $headers")

        // the receiving side: extract into the root context and continue the trace
        val remote = propagator.extract(rootContext(), headers, createMapTextMapGetter())
        remote.runWithContext {
            val server = tracer.startSpan("inbound-handler", spanKind = SpanKind.SERVER)
            println("Server span parent: ${server.parent.spanId}, tenant: ${currentContext().extractBaggage().getValue("tenant")}")
            server.end()
        }
    }
    println("After runWithContext, request.id is ${currentContext().get(REQUEST_ID)}")
}
