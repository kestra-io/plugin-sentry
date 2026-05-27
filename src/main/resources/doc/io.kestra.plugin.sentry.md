# How to use the Sentry plugin

Capture errors and send execution summaries to Sentry from Kestra flows.

## Authentication

Set `dsn` to your Sentry project's DSN (found in Settings → Projects → Client Keys). Store it in a [secret](https://kestra.io/docs/concepts/secret).

## Tasks

`SentryAlert` sends an event to Sentry as a step within a flow — set `payload` to a JSON body in the Sentry event format, or omit it to send a default alert. Set `endpointType` to `ENVELOPE` (the default) or `STORE` to match your Sentry SDK configuration.

`SentryExecution` sends a structured execution summary including status, duration, and an execution link, and is designed for use with a [Flow trigger](https://kestra.io/docs/workflow-components/triggers) in a dedicated monitoring namespace that watches other namespaces for failures.
