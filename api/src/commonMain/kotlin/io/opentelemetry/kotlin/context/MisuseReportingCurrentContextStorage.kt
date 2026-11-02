package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.error.SdkError
import io.opentelemetry.kotlin.error.SdkErrorSeverity
import io.opentelemetry.kotlin.error.reportApiUserCodeError
import io.opentelemetry.kotlin.error.reportError

private const val ALREADY_DETACHED_MSG = "Scope.detach() called on an already-detached scope"
private const val OUT_OF_ORDER_MSG = "Scope.detach() called out of order - the current context has changed since " +
    "this scope was attached"
private const val SAME_CONTEXT_MSG = "Context.attach() called with the context that is already current"

/**
 * Wraps every installed [CurrentContextStorage]. It reports scope misuse, and exceptions thrown by
 * the wrapped storage, through the process-wide error sink.
 *
 * It only reports: it never skips an attach or a detach, and it does not assume that
 * `attach(ctx)` makes `ctx` current, so layered storages work through it.
 */
@OptIn(ExperimentalApi::class)
internal class MisuseReportingCurrentContextStorage(
    private val delegate: CurrentContextStorage,
) : CurrentContextStorage {

    override fun current(): Context = try {
        delegate.current()
    } catch (e: Throwable) {
        reportStorageFailure("current", e)
        rootContext()
    }

    override fun attach(context: Context): Scope {
        if (current() == context) {
            reportMisuse("Context.attach", SAME_CONTEXT_MSG)
        }
        val scope = try {
            delegate.attach(context)
        } catch (e: Throwable) {
            reportStorageFailure("attach", e)
            return UnattachedScope
        }
        return MisuseReportingScope(scope, current(), this)
    }

    fun reportStorageFailure(operation: String, e: Throwable) {
        reportApiUserCodeError(e, "CurrentContextStorage.$operation threw")
    }

    fun reportMisuse(api: String, message: String) {
        reportError(
            SdkError.ApiMisuse(
                api = api,
                message = message,
                severity = SdkErrorSeverity.WARNING,
            )
        )
    }

    /**
     * Detaches [delegate] at most once. Out-of-order detach is detected by comparing the current
     * context against [attachedState], the storage's current context straight after the attach.
     */
    private class MisuseReportingScope(
        private val delegate: Scope,
        private val attachedState: Context,
        private val storage: MisuseReportingCurrentContextStorage,
    ) : Scope {

        private val detached = ContextStorageAtomicRef<Any?>(null)

        override fun detach(): Boolean {
            if (!detached.compareAndSet(null, DETACHED)) {
                storage.reportMisuse("Scope.detach", ALREADY_DETACHED_MSG)
                return false
            }
            if (storage.current() != attachedState) {
                storage.reportMisuse("Scope.detach", OUT_OF_ORDER_MSG)
            }
            return try {
                delegate.detach()
            } catch (e: Throwable) {
                storage.reportStorageFailure("detach", e)
                false
            }
        }

        private companion object {
            val DETACHED = Any()
        }
    }
}
