# config-dsl

This module records what the programmatic DSL declared as a `config-model`, so that the DSL can
take part in configuration precedence alongside the other mechanisms.

It holds the implementations of the DSL interfaces declared in `sdk-api`, shared by both
`implementation` and `compat`. A limit nobody assigned is left unset rather than defaulted, which
is what allows a lower-precedence mechanism to supply it.
