# config

This module resolves the configuration supplied by every mechanism into the single
`config-model` the SDK is initialized with, and is the only config module `implementation` and
`compat` depend on.

It gathers the layers from `config-dsl`, `config-envar` and `config-yaml`, then applies the
precedence rules defined in `config-model`.

https://opentelemetry.io/docs/specs/otel/configuration/
