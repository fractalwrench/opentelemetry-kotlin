package io.opentelemetry.kotlin.smoketest

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.NoopOpenTelemetry
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.createCompatOpenTelemetry
import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.propagation.Propagators
import io.opentelemetry.kotlin.propagation.TextMapGetter
import io.opentelemetry.kotlin.propagation.TextMapSetter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Steel thread for issue #851: instrumentation can obtain a working W3C baggage propagator from an
 * [OpenTelemetry] instance without the application having configured one, identically on both
 * backends, and gets nothing when the SDK is switched off.
 */
@OptIn(ExperimentalApi::class)
internal class PropagatorApiSmokeTest {

    private val seed = mapOf("baggage" to "key1=value1,key2=value2")

    @Test
    fun `instrumentation can propagate baggage when the SDK configured no propagator`() {
        backends().forEach { (name, otel) ->
            assertTrue(otel.propagator.fields().isEmpty(), "$name: configured propagator is a no-op")

            val propagator = Propagators.create().w3cBaggage()
            val extracted = propagator.extract(otel.context.root(), seed, Getter)
            assertEquals("value1", extracted.extractBaggage().getValue("key1"), "$name: extract")

            val carrier = mutableMapOf<String, String>()
            propagator.inject(extracted, carrier, Setter)
            assertEquals(seed["baggage"], carrier["baggage"], "$name: re-inject")
        }
    }

    @Test
    fun `both backends produce identical baggage headers`() {
        val (_, impl) = backends()[0]
        val (_, compat) = backends()[1]
        assertEquals(roundTrip(compat), roundTrip(impl))
    }

    @Test
    fun `a switched-off SDK propagates nothing`() {
        val propagator = Propagators.create().w3cBaggage()
        val context = NoopOpenTelemetry.context.root()

        assertEquals(context, propagator.extract(context, seed, Getter), "extract should be inert")

        val carrier = mutableMapOf<String, String>()
        propagator.inject(context, carrier, Setter)
        assertTrue(carrier.isEmpty(), "inject should write nothing")
    }

    private fun backends(): List<Pair<String, OpenTelemetry>> = listOf(
        "implementation" to createOpenTelemetry(),
        "compat" to createCompatOpenTelemetry(),
    )

    private fun roundTrip(otel: OpenTelemetry): Map<String, String> {
        val propagator = Propagators.create().w3cBaggage()
        val context = propagator.extract(otel.context.root(), seed, Getter)
        val carrier = mutableMapOf<String, String>()
        propagator.inject(context, carrier, Setter)
        return carrier
    }

    private object Getter : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>): Collection<String> = carrier.keys
        override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
        override fun getAll(carrier: Map<String, String>?, key: String): List<String> =
            listOfNotNull(carrier?.get(key))
    }

    private object Setter : TextMapSetter<MutableMap<String, String>> {
        override fun set(carrier: MutableMap<String, String>?, key: String, value: String) {
            carrier?.put(key, value)
        }
    }
}
