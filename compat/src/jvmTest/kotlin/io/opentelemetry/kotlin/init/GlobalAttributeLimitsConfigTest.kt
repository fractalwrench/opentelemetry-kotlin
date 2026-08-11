package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.clock.FakeClock
import io.opentelemetry.kotlin.config.dsl.AttributeLimitsConfigModelBuilder
import io.opentelemetry.kotlin.config.model.resolve
import io.opentelemetry.kotlin.error.NoopSdkErrorHandler
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GlobalAttributeLimitsConfigTest {

    private val clock = FakeClock()

    @Test
    fun `AttributeLimitsConfigModelBuilder default state`() {
        val cfg = AttributeLimitsConfigModelBuilder()

        assertNull(cfg.configuredAttributeCountLimit)
        assertNull(cfg.configuredAttributeValueLengthLimit)
        assertEquals(DEFAULT_ATTR_LIMIT, cfg.attributeCountLimit)
        assertEquals(DEFAULT_ATTR_VALUE_LENGTH_LIMIT, cfg.attributeValueLengthLimit)
    }

    @Test
    fun `AttributeLimitsConfigModelBuilder records assignment`() {
        val cfg = AttributeLimitsConfigModelBuilder()
        cfg.attributeCountLimit = 64
        cfg.attributeValueLengthLimit = 256

        assertEquals(64, cfg.configuredAttributeCountLimit)
        assertEquals(256, cfg.configuredAttributeValueLengthLimit)
    }

    @Test
    fun `global only - applies to spans and logs`() {
        val globalLimits = globalLimits { attributeCountLimit = 64 }

        val tracerConfig = CompatTracerProviderConfig(clock, NoopSdkErrorHandler)
        assertEquals(64, tracerConfig.spanLimitsModel(globalLimits).resolve().attributeCountLimit)

        val loggerConfig = CompatLoggerProviderConfig(clock, NoopSdkErrorHandler)
        loggerConfig.build(clock, globalLimits = globalLimits)
        assertEquals(64, loggerConfig.logLimitsConfig.attributeCountLimit)
    }

    @Test
    fun `signal-specific overrides global`() {
        val globalLimits = globalLimits { attributeCountLimit = 64 }

        val tracerConfig = CompatTracerProviderConfig(clock, NoopSdkErrorHandler).apply {
            spanLimits { attributeCountLimit = 32 }
        }
        assertEquals(32, tracerConfig.spanLimitsModel(globalLimits).resolve().attributeCountLimit)

        val loggerConfig = CompatLoggerProviderConfig(clock, NoopSdkErrorHandler)
        loggerConfig.build(clock, globalLimits = globalLimits)
        assertEquals(64, loggerConfig.logLimitsConfig.attributeCountLimit)
    }

    @Test
    fun `partial signal override - other global properties still apply`() {
        val globalLimits = globalLimits { attributeCountLimit = 64 }

        val tracerConfig = CompatTracerProviderConfig(clock, NoopSdkErrorHandler).apply {
            spanLimits { attributeValueLengthLimit = 256 }
        }

        with(tracerConfig.spanLimitsModel(globalLimits).resolve()) {
            assertEquals(64, attributeCountLimit)
            assertEquals(256, attributeValueLengthLimit)
        }
    }

    @Test
    fun `no global - specification defaults apply`() {
        val tracerConfig = CompatTracerProviderConfig(clock, NoopSdkErrorHandler)

        with(tracerConfig.spanLimitsModel().resolve()) {
            assertEquals(DEFAULT_ATTR_LIMIT, attributeCountLimit)
            assertEquals(DEFAULT_ATTR_VALUE_LENGTH_LIMIT, attributeValueLengthLimit)
        }

        val loggerConfig = CompatLoggerProviderConfig(clock, NoopSdkErrorHandler)
        loggerConfig.build(clock)
        assertEquals(DEFAULT_ATTR_LIMIT, loggerConfig.logLimitsConfig.attributeCountLimit)
        assertEquals(DEFAULT_ATTR_VALUE_LENGTH_LIMIT, loggerConfig.logLimitsConfig.attributeValueLengthLimit)
    }

    private fun globalLimits(action: AttributeLimitsConfigModelBuilder.() -> Unit) =
        AttributeLimitsConfigModelBuilder().apply(action)
}
