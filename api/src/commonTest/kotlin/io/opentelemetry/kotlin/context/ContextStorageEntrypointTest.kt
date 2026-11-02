package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.error.RecordingSdkErrorHandler
import io.opentelemetry.kotlin.error.setProcessSdkErrorHandler
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalApi::class, ProcessWideStateTestingApi::class)
internal class ContextStorageEntrypointTest {

    private val key = createContextKey<String>("key")

    @AfterTest
    fun tearDown() {
        resetContextStorageForTesting()
        setProcessSdkErrorHandler(null)
    }

    @Test
    fun testNoStorageInstalled() {
        assertSame(rootContext(), currentContext())
        val ctx = rootContext().set(key, "value")
        assertTrue(ctx.attach().detach())
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testRunWithContextWithoutStorageStillRunsBlock() {
        val result = rootContext().set(key, "value").runWithContext { 42 }
        assertEquals(42, result)
    }

    @Test
    fun testFirstInstallWins() {
        val first = SlotStorage()
        val second = SlotStorage()
        assertTrue(installContextStorage(first))
        assertFalse(installContextStorage(second))

        val ctx = rootContext().set(key, "value")
        ctx.attach()
        assertSame(ctx, first.current())
        assertSame(rootContext(), second.current())
    }

    @Test
    fun testUninstallByNonOwner() {
        val owner = SlotStorage()
        installContextStorage(owner)
        assertFalse(uninstallContextStorage(SlotStorage()))
        assertTrue(uninstallContextStorage(owner))
        assertFalse(uninstallContextStorage(owner))
    }

    @Test
    fun testResetEmptiesSlot() {
        installContextStorage(SlotStorage())
        resetContextStorageForTesting()
        assertTrue(installContextStorage(SlotStorage()))
    }

    @Test
    fun testAttachAndDetach() {
        installContextStorage(SlotStorage())
        val ctx = rootContext().set(key, "value")
        val scope = ctx.attach()
        assertSame(ctx, currentContext())
        assertTrue(scope.detach())
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testRunWithContext() {
        installContextStorage(SlotStorage())
        val ctx = rootContext().set(key, "value")
        val observed = ctx.runWithContext { currentContext().get(key) }
        assertEquals("value", observed)
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testRunWithContextRestoresOnThrow() {
        installContextStorage(SlotStorage())
        val ctx = rootContext().set(key, "value")
        assertFailsWith<IllegalStateException> {
            ctx.runWithContext { error("boom") }
        }
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testUninstallReturnsToRoot() {
        val storage = SlotStorage()
        installContextStorage(storage)
        rootContext().set(key, "value").attach()
        uninstallContextStorage(storage)
        assertSame(rootContext(), currentContext())
    }

    @Test
    fun testErrorsBeforeHandlerInstalledAreReplayed() {
        installContextStorage(SlotStorage())
        val scope = rootContext().set(key, "value").attach()
        scope.detach()
        scope.detach()

        val handler = RecordingSdkErrorHandler()
        setProcessSdkErrorHandler(handler)
        assertEquals(1, handler.misuses.size)
    }
}
