# config-envar

This module reads the OTel environment variables into a `config-model`, so that the environment
can take part in configuration precedence alongside the other mechanisms.

Only the JVM reads the process environment; every other target reports no environment
configuration.

https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/
