# Kestra Sentry Plugin

## What

- Provides plugin components under `io.kestra.plugin.sentry`.
- Includes classes such as `ErrorLevel`, `SentryTemplate`, `SentryExecution`, `Platform`.

## Why

- This plugin integrates Kestra with Sentry.
- It provides tasks that send events to Sentry.

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
