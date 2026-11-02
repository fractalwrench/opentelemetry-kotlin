package io.opentelemetry.kotlin.integration.test

import io.opentelemetry.kotlin.aliases.OtelJavaBaggage
import io.opentelemetry.kotlin.aliases.OtelJavaContext
import io.opentelemetry.kotlin.aliases.OtelJavaOpenTelemetry
import io.opentelemetry.kotlin.aliases.OtelJavaSpan
import io.opentelemetry.kotlin.baggage.createBaggage
import io.opentelemetry.kotlin.context.createOtelJavaCurrentContextStorage
import io.opentelemetry.kotlin.context.currentContext
import io.opentelemetry.kotlin.context.rootContext
import io.opentelemetry.kotlin.error.FakeSdkErrorHandler
import io.opentelemetry.kotlin.toOtelJavaApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Verifies that opting in to opentelemetry-java's context storage shares a single current
 * context between the Kotlin API and the static opentelemetry-java context APIs.
 */
internal class OtelJavaContextStorageTest {

    private lateinit var harness: IntegrationTestHarness
    private lateinit var errorHandler: FakeSdkErrorHandler
    private lateinit var javaApi: OtelJavaOpenTelemetry

    @BeforeTest
    fun setUp() = runTest {
        errorHandler = FakeSdkErrorHandler()
        harness = IntegrationTestHarness(testScheduler) {
            errorHandler(errorHandler)
            context { storage = createOtelJavaCurrentContextStorage() }
        }
        javaApi = harness.kotlinApi.toOtelJavaApi()
    }

    @AfterTest
    fun tearDown() {
        try {
            assertSame(OtelJavaContext.root(), OtelJavaContext.current())
        } finally {
            OtelJavaContext.root().makeCurrent()
            harness.shutdown()
        }
    }

    @Test
    fun testNoWarningWhenOptedIn() {
        assertTrue(errorHandler.apiMisuses.isEmpty())
    }

    @Test
    fun testCurrentIsRootByDefault() {
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testKotlinAttachVisibleToJavaStaticApis() {
        val span = harness.tracer.startSpan("kotlin_span")
        val ctx = currentContext()
            .storeSpan(span)
            .storeBaggage(createBaggage { put("key", "value") })
        val scope = ctx.attach()
        try {
            assertEquals(span.spanContext.spanId, OtelJavaSpan.current().spanContext.spanId)
            assertEquals("value", OtelJavaBaggage.current().getEntryValue("key"))
        } finally {
            assertTrue(scope.detach())
        }
        span.end()
        assertSame(OtelJavaContext.root(), OtelJavaContext.current())
    }

    @Test
    fun testJavaMakeCurrentParentsKotlinSpan() = runTest {
        val javaSpan = javaApi.tracerProvider.get("tracer").spanBuilder("java_span").startSpan()
        OtelJavaContext.current().with(javaSpan).makeCurrent().use {
            assertEquals(javaSpan.spanContext.spanId, currentContext().extractSpan().spanContext.spanId)
            harness.tracer.startSpan("kotlin_span").end()
        }
        javaSpan.end()

        harness.assertSpans(2) { spans ->
            val child = spans.single { it.name == "kotlin_span" }
            assertEquals(javaSpan.spanContext.spanId, child.parent.spanId)
        }
    }

    @Test
    fun testJavaBaggageVisibleToKotlin() {
        OtelJavaBaggage.builder().put("key", "value").build().makeCurrent().use {
            assertEquals("value", currentContext().extractBaggage().asMap()["key"]?.value)
        }
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testNestedScopesRestoreInOrder() {
        val outer = harness.tracer.startSpan("outer")
        val inner = harness.tracer.startSpan("inner")
        val outerScope = rootContext().storeSpan(outer).attach()
        val innerScope = currentContext().storeSpan(inner).attach()

        assertEquals(inner.spanContext.spanId, OtelJavaSpan.current().spanContext.spanId)
        assertTrue(innerScope.detach())
        assertEquals(outer.spanContext.spanId, OtelJavaSpan.current().spanContext.spanId)
        assertTrue(outerScope.detach())
        inner.end()
        outer.end()
    }

    @Test
    fun testOutOfOrderDetachReportsMisuse() {
        val outerScope = rootContext().storeSpan(harness.tracer.startSpan("outer")).attach()
        val innerScope = currentContext().storeSpan(harness.tracer.startSpan("inner")).attach()

        // reported, and passed through to opentelemetry-java, which ignores an out-of-order close
        outerScope.detach()
        assertEquals("Scope.detach", errorHandler.apiMisuses.single().api)
        assertTrue(OtelJavaSpan.current().spanContext.isValid)

        // detaching inner is in order again, and restores outer
        assertTrue(innerScope.detach())
        assertEquals(1, errorHandler.apiMisuses.size)
        OtelJavaContext.root().makeCurrent()
    }
}
