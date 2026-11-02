package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.error.SdkErrorHandler

/**
 * Implemented by SDK instances so that other modules (e.g. compat's `toOtelJavaApi()`) can see
 * what the instance claimed in the process-wide slots at init.
 */
@OptIn(ExperimentalApi::class)
public interface ProcessWideStateOwner {

    /**
     * The storage that this SDK installed into the process-wide slot, or null if it didn't install
     * one.
     */
    public val installedStorage: CurrentContextStorage?

    /**
     * The error handler this SDK was configured with.
     */
    public val sdkErrorHandler: SdkErrorHandler
}
