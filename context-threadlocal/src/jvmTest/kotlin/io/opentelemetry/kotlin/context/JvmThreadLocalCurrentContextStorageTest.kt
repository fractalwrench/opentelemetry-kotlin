package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertSame

@OptIn(ExperimentalApi::class)
internal class JvmThreadLocalCurrentContextStorageTest {

    @Test
    fun testContextIsIsolatedPerThread() {
        val key = createContextKey<String>("key")
        val storage = createThreadLocalCurrentContextStorage()
        val mainContext = rootContext().set(key, "main")
        val scope = storage.attach(mainContext)

        val observedFromOtherThread = AtomicReference<Context>()
        val thread = Thread {
            observedFromOtherThread.set(storage.current())
            storage.attach(rootContext().set(key, "other"))
        }
        thread.start()
        thread.join()

        assertSame(rootContext(), observedFromOtherThread.get())
        assertSame(mainContext, storage.current())
        scope.detach()
    }
}
