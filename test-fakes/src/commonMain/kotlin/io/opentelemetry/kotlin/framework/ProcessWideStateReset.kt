package io.opentelemetry.kotlin.framework

import io.opentelemetry.kotlin.context.ProcessWideStateTestingApi
import io.opentelemetry.kotlin.context.resetContextStorageForTesting
import io.opentelemetry.kotlin.error.setProcessSdkErrorHandler

/**
 * Uninstalls the process-wide CurrentContextStorage and SdkErrorHandler, and discards buffered
 * errors. Call it in `@AfterTest`, never in a `@BeforeTest` that runs alongside another
 * `@BeforeTest` that creates an SDK.
 */
@OptIn(ProcessWideStateTestingApi::class)
fun resetProcessWideState() {
    resetContextStorageForTesting()
    setProcessSdkErrorHandler(null)
}
