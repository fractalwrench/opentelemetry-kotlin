# config-yaml

This module parses declarative configuration YAML into the `config-schema` types, and maps those
onto a `config-model` so that a declarative config file can take part in configuration precedence
alongside the other mechanisms.

Locating and reading the file is not implemented yet: callers supply an already-parsed document.

https://opentelemetry.io/docs/specs/otel/configuration/data-model/
