package io.opentelemetry.kotlin.logging

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.attributes.MutableAttributeContainer
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.logging.model.FakeReadableLogRecord
import io.opentelemetry.kotlin.logging.model.SeverityNumber

@OptIn(ExperimentalApi::class)
class FakeLogger(
    val name: String,
    var enabledResult: () -> Boolean = { true },
) : Logger {

    val logs: MutableList<FakeReadableLogRecord> = mutableListOf()

    override fun enabled(
        context: Context?,
        severityNumber: SeverityNumber?,
        eventName: String?,
    ): Boolean = enabledResult()

    override fun emit(
        body: String?,
        eventName: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        logs.add(
            FakeReadableLogRecord(
                timestamp,
                observedTimestamp,
                severityNumber,
                severityText,
                body,
                eventName,
            )
        )
    }

    @Deprecated(
        "Deprecated",
        replaceWith = ReplaceWith("emit(body, null, timestamp, observedTimestamp, context, severityNumber, severityText, attributes)")
    )
    override fun log(
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        throw UnsupportedOperationException()
    }

    @Deprecated(
        "Deprecated",
        replaceWith = ReplaceWith("emit(body, eventName, timestamp, observedTimestamp, context, severityNumber, severityText, attributes)")
    )
    override fun logEvent(
        eventName: String,
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        throw UnsupportedOperationException()
    }
}
