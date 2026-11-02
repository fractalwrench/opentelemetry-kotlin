package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.error.SdkError
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorSeverity
import io.opentelemetry.kotlin.error.setProcessSdkErrorHandler

/**
 * What one SDK instance installed into the process-wide slots at init. [release] uninstalls
 * only what this instance installed, so shutting down one SDK never removes another's storage or
 * error handler.
 */
@OptIn(ExperimentalApi::class)
public class ProcessWideRegistration private constructor(
    private val installedStorage: CurrentContextStorage?,
    private val installedHandler: SdkErrorHandler?,
) {

    /**
     * True if this SDK installed a storage it chose by default, i.e. nothing was configured and
     * the slot was empty.
     */
    public var installedDefaultStorage: Boolean = false
        private set

    /**
     * Uninstalls the storage and error handler, if this instance installed them.
     */
    public fun release() {
        installedStorage?.let(::uninstallContextStorage)
        installedHandler?.let { setProcessSdkErrorHandler(null) }
    }

    public companion object {

        /**
         * Installs [errorHandler] into the process-wide error sink, then installs [configured] (or
         * [default], if nothing was configured) as the process-wide [CurrentContextStorage].
         *
         * If the storage slot is already taken, the installed storage is shared. A configured
         * storage that loses this way is reported to [errorHandler] as a WARNING; a default one
         * is not, because the user didn't ask for it.
         */
        public fun register(
            configured: CurrentContextStorage?,
            default: () -> CurrentContextStorage,
            errorHandler: SdkErrorHandler,
            ignoredStorageMessage: String = "A CurrentContextStorage is already installed in this process, so the " +
                "configured storage was ignored. The SDK uses the installed storage instead.",
        ): ProcessWideRegistration {
            val handler = errorHandler.takeIf(::setProcessSdkErrorHandler)
            val storage = configured ?: default()
            val installed = installContextStorage(storage)
            if (!installed && configured != null) {
                errorHandler.onError(
                    SdkError.ApiMisuse(
                        api = "ContextConfigDsl.storage",
                        message = ignoredStorageMessage,
                        severity = SdkErrorSeverity.WARNING,
                    )
                )
            }
            return ProcessWideRegistration(storage.takeIf { installed }, handler).apply {
                installedDefaultStorage = installed && configured == null
            }
        }
    }
}

/**
 * Implemented by SDK instances so that other modules (e.g. compat's `toOtelJavaApi()`) can see
 * what the instance installed into the process-wide slots.
 */
public interface ProcessWideRegistrationOwner {
    public val processWideRegistration: ProcessWideRegistration?
}
