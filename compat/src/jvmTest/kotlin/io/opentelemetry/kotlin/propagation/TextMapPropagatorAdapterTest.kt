package io.opentelemetry.kotlin.propagation

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.aliases.OtelJavaBaggage
import io.opentelemetry.kotlin.aliases.OtelJavaContext
import io.opentelemetry.kotlin.aliases.OtelJavaTextMapGetter
import io.opentelemetry.kotlin.aliases.OtelJavaTextMapPropagator
import io.opentelemetry.kotlin.baggage.createBaggage
import io.opentelemetry.kotlin.context.ContextAdapter
import io.opentelemetry.kotlin.factory.ContextFactoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalApi::class)
internal class TextMapPropagatorAdapterTest {

    private val impl: OtelJavaTextMapPropagator = W3CBaggagePropagator.getInstance()
    private val kotlinContextFactory = ContextFactoryImpl()

    @Test
    fun `fields delegates to wrapped Java propagator`() {
        val adapter = TextMapPropagatorAdapter(impl)
        assertEquals(impl.fields().toList(), adapter.fields().toList())
    }

    @Test
    fun `inject writes baggage header into carrier`() {
        val javaCtx = OtelJavaBaggage.builder()
            .put("k", "v")
            .build()
            .storeInContext(OtelJavaContext.root())
        val ktCtx = ContextAdapter(javaCtx)
        val carrier = mutableMapOf<String, String>()

        TextMapPropagatorAdapter(impl).inject(ktCtx, carrier, MapSetter)

        assertEquals("k=v", carrier["baggage"])
    }

    @Test
    fun `extract reads baggage header from carrier`() {
        val carrier = mapOf("baggage" to "k=v")

        val extracted = TextMapPropagatorAdapter(impl).extract(
            ContextAdapter(OtelJavaContext.root()),
            carrier,
            MapGetter
        )

        val javaCtx = (extracted as ContextAdapter).impl
        assertEquals("v", OtelJavaBaggage.fromContext(javaCtx).getEntryValue("k"))
    }

    @Test
    fun `extract returns ContextAdapter wrapping Java context`() {
        val extracted = TextMapPropagatorAdapter(impl).extract(
            ContextAdapter(OtelJavaContext.root()),
            mapOf("baggage" to "k=v"),
            MapGetter
        )
        assertTrue(extracted is ContextAdapter)
    }

    @Test
    fun `extract on malformed input returns input context unchanged`() {
        val input = ContextAdapter(OtelJavaContext.root())
        val extracted = TextMapPropagatorAdapter(impl).extract(
            input,
            mapOf("baggage" to ""),
            MapGetter
        )
        val javaCtx = (extracted as ContextAdapter).impl
        assertTrue(OtelJavaBaggage.fromContext(javaCtx).isEmpty)
    }

    @Test
    fun `inject writes baggage from a Kotlin-native context`() {
        val ctx = kotlinContextFactory.root().storeBaggage(createBaggage { put("k", "v") })
        val carrier = mutableMapOf<String, String>()

        TextMapPropagatorAdapter(impl).inject(ctx, carrier, MapSetter)

        assertEquals("k=v", carrier["baggage"])
    }

    @Test
    fun `extract into a Kotlin-native context preserves its type`() {
        val input = kotlinContextFactory.root()

        val extracted = TextMapPropagatorAdapter(impl).extract(input, mapOf("baggage" to "k=v"), MapGetter)

        assertFalse(extracted is ContextAdapter)
        assertEquals("v", extracted.extractBaggage().getValue("k"))
    }

    @Test
    fun `getter adapter delegates keys to wrapped Kotlin getter`() {
        val carrier = mapOf("a" to "1", "b" to "2")
        val adapter = OtelJavaTextMapGetterAdapter(MapGetter)
        assertEquals(carrier.keys, adapter.keys(carrier).toSet())
    }

    @Test
    fun `setter adapter is a no-op when carrier is null`() {
        val throwingDelegate = TextMapSetter<MutableMap<String, String>> { _, _, _ ->
            error("delegate must not be invoked when carrier is null")
        }
        val adapter = OtelJavaTextMapSetterAdapter(throwingDelegate)
        adapter.set(null, "k", "v")
    }

    @Test
    fun `kotlin getter adapter returns single value list from getAll when key present`() {
        val carrier = mapOf("a" to "1")
        val adapter = TextMapGetterAdapter(JavaMapGetter)
        assertEquals(listOf("1"), adapter.getAll(carrier, "a"))
    }

    @Test
    fun `kotlin getter adapter returns empty list from getAll when key absent`() {
        val carrier = mapOf("a" to "1")
        val adapter = TextMapGetterAdapter(JavaMapGetter)
        assertTrue(adapter.getAll(carrier, "missing").isEmpty())
    }

    @Test
    fun `getter adapter returns null when carrier is null`() {
        val throwingDelegate = object : TextMapGetter<Map<String, String>> {
            val errMsg = "delegate must not be invoked when carrier is null"

            override fun keys(carrier: Map<String, String>): Collection<String> {
                error(errMsg)
            }

            override fun get(carrier: Map<String, String>, key: String): String {
                error(errMsg)
            }

            override fun getAll(carrier: Map<String, String>, key: String): List<String> {
                error(errMsg)
            }
        }
        val adapter = OtelJavaTextMapGetterAdapter(throwingDelegate)

        assertNull(adapter.get(null, "k"))
    }

    @Test
    fun `inject swallows a throwing Kotlin setter`() {
        val javaCtx = OtelJavaBaggage.builder()
            .put("k", "v")
            .build()
            .storeInContext(OtelJavaContext.root())
        TextMapPropagatorAdapter(impl).inject(ContextAdapter(javaCtx), mutableMapOf(), ThrowingTextMapSetter)
    }

    @Test
    fun `extract returns input context when the Kotlin getter throws`() {
        val input = ContextAdapter(OtelJavaContext.root())
        val extracted = TextMapPropagatorAdapter(impl).extract(input, mapOf("baggage" to "k=v"), ThrowingTextMapGetter)
        assertSame(input, extracted)
    }

    @Test
    fun `Kotlin propagator exposed to Java swallows anything it throws`() {
        val adapter = OtelJavaTextMapPropagatorAdapter(ThrowingTextMapPropagator)
        val javaCtx = OtelJavaContext.root()
        assertTrue(adapter.fields().isEmpty())
        adapter.inject(javaCtx, mutableMapOf<String, String>()) { _, _, _ -> }
        assertSame(javaCtx, adapter.extract(javaCtx, emptyMap<String, String>(), JavaMapGetter))
    }

    private object MapSetter : TextMapSetter<MutableMap<String, String>> {
        override fun set(carrier: MutableMap<String, String>, key: String, value: String) {
            carrier[key] = value
        }
    }

    private object MapGetter : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>): Collection<String> = carrier.keys
        override fun get(carrier: Map<String, String>, key: String): String? = carrier[key]
        override fun getAll(carrier: Map<String, String>, key: String): List<String> =
            carrier[key]?.let { listOf(it) } ?: emptyList()
    }

    private object JavaMapGetter : OtelJavaTextMapGetter<Map<String, String>> {
        override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
    }
}
