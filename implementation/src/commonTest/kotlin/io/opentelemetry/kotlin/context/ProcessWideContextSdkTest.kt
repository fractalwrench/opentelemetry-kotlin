package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.OpenTelemetrySdk
import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.error.FakeSdkErrorHandler
import io.opentelemetry.kotlin.error.SdkError
import io.opentelemetry.kotlin.error.SdkErrorSeverity
import io.opentelemetry.kotlin.error.reportError
import io.opentelemetry.kotlin.framework.resetProcessWideState
import io.opentelemetry.kotlin.tracing.createSpanContext
import io.opentelemetry.kotlin.tracing.export.FakeSpanProcessor
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * How SDK instances interact with the process-wide context storage and error sink.
 */
@OptIn(ExperimentalApi::class)
internal class ProcessWideContextSdkTest {

    private val key = createContextKey<String>("key")

    @AfterTest
    fun tearDown() {
        resetProcessWideState()
    }

    @Test
    fun testCurrentContextReflectsAttach() = runTest {
        val sdk = createOpenTelemetry() as OpenTelemetrySdk
        val span = sdk.tracerProvider.getTracer("test").startSpan("span")
        val ctx = rootContext().storeSpan(span)
        val scope = ctx.attach()
        assertSame(ctx, currentContext())
        scope.detach()
        assertSame(rootContext(), currentContext())
        sdk.shutdown()
    }

    @Test
    fun testImplicitParentInsideRunWithContext() = runTest {
        val sdk = createOpenTelemetry { tracerProvider { export { FakeSpanProcessor() } } } as OpenTelemetrySdk
        val tracer = sdk.tracerProvider.getTracer("test")
        val parent = tracer.startSpan("parent")
        val child = rootContext().storeSpan(parent).runWithContext {
            tracer.startSpan("child")
        }
        assertTrue(parent.spanContext.isValid)
        assertEquals(parent.spanContext.traceId, child.spanContext.traceId)
        sdk.shutdown()
    }

    @Test
    fun testSecondConfiguredSdkWarnsAndSharesStorage() = runTest {
        val first = createOpenTelemetry() as OpenTelemetrySdk
        val handler = FakeSdkErrorHandler()
        val configured = FakeCurrentContextStorage()
        val second = createOpenTelemetry {
            errorHandler(handler)
            context { storage = configured }
        } as OpenTelemetrySdk

        val misuse = handler.apiMisuses.single()
        assertEquals("ContextConfigDsl.storage", misuse.api)
        assertEquals(SdkErrorSeverity.WARNING, misuse.severity)

        rootContext().set(key, "value").runWithContext {
            assertEquals("value", currentContext().get(key))
        }
        assertTrue(configured.attached.isEmpty())
        second.shutdown()
        first.shutdown()
    }

    @Test
    fun testOnlyOwnerShutdownEmptiesSlot() = runTest {
        val owner = createOpenTelemetry() as OpenTelemetrySdk
        val sharer = createOpenTelemetry() as OpenTelemetrySdk

        sharer.shutdown()
        val scope = rootContext().set(key, "value").attach()
        assertEquals("value", currentContext().get(key))
        scope.detach()

        owner.shutdown()
        rootContext().set(key, "value").attach()
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testPreInstalledStorageWinsSilently() = runTest {
        val storage = FakeCurrentContextStorage()
        installContextStorage(storage)
        val handler = FakeSdkErrorHandler()
        val sdk = createOpenTelemetry { errorHandler(handler) } as OpenTelemetrySdk

        val ctx = rootContext().set(key, "value")
        ctx.runWithContext {}
        assertEquals(listOf(ctx), storage.attached)
        assertTrue(handler.errors.isEmpty())

        sdk.shutdown()
        assertTrue(uninstallContextStorage(storage), "a storage the SDK didn't install survives its shutdown")
    }

    @Test
    fun testOutOfOrderDetachReachesOwningSdkHandler() = runTest {
        val handler = FakeSdkErrorHandler()
        val sdk = createOpenTelemetry { errorHandler(handler) } as OpenTelemetrySdk
        val outer = rootContext().set(key, "outer").attach()
        val inner = rootContext().set(key, "inner").attach()
        outer.detach()
        inner.detach()
        assertEquals("Scope.detach", handler.apiMisuses.first().api)
        sdk.shutdown()
    }

    @Test
    fun testApiErrorsBeforeInitAreReplayedToSdkHandler() = runTest {
        createSpanContext("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331") { error("boom") }

        val handler = FakeSdkErrorHandler()
        val sdk = createOpenTelemetry { errorHandler(handler) } as OpenTelemetrySdk
        assertEquals("boom", handler.userCodeErrors.single().cause.message)

        reportError(SdkError.ApiMisuse("test", "direct", SdkErrorSeverity.WARNING))
        assertEquals("direct", handler.apiMisuses.single().message)
        sdk.shutdown()
    }
}
