# Kestra Sentry Plugin

## What

- Provides plugin components under `io.kestra.plugin.sentry`.
- Includes classes such as `ErrorLevel`, `SentryTemplate`, `SentryExecution`, `Platform`.

## Why

- What user problem does this solve? Teams need to send events to Sentry from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Sentry steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Sentry.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `sentry`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.sentry.SentryAlert`
- `io.kestra.plugin.sentry.SentryExecution`

### Project Structure

```
plugin-sentry/
├── src/main/java/io/kestra/plugin/sentry/
├── src/test/java/io/kestra/plugin/sentry/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
