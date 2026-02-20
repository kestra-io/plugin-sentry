package io.kestra.plugin.sentry;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.plugins.notifications.ExecutionInterface;
import io.kestra.core.plugins.notifications.ExecutionService;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send Sentry alert with execution data",
    description = """
    Sends execution metadata (ID, namespace, flow, start time, duration, status, failing task) with a UI link to Sentry. Use in flows triggered by Flow triggers; for `errors` handlers prefer `SentryAlert`. Requires a project DSN (how to find it: [Sentry DSN guide](https://docs.sentry.io/product/sentry-basics/concepts/dsn-explainer/#where-to-find-your-dsn)); level defaults to ERROR and payload remains editable through inherited properties (payload reference: [Sentry event payloads](https://develop.sentry.dev/sdk/event-payloads/))."""
)
@Plugin(
    examples = {
        @Example(
            title = "This monitoring flow is triggered anytime a flow fails in the `prod` namespace. It then sends a Sentry alert with the execution information. You can fully customize the [trigger conditions](https://kestra.io/plugins/core#conditions).",
            full = true,
            code = """
                id: failure_alert
                namespace: company.team

                tasks:
                  - id: send_alert
                    type: io.kestra.plugin.sentry.SentryExecution
                    transaction: "/execution/id/{{ trigger.executionId }}"
                    dsn: "{{ secret('SENTRY_DSN') }}"
                    level: ERROR
                    executionId: "{{ trigger.executionId }}"
                    customFields:
                      shard: "{{ flow.namespace | split('.') | last }}"
                      retried: "{{ execution.state.current == 'RETRY' }}"
                    customMessage: "Failure in prod namespace: {{ trigger.executionId }}"
                    options:
                      readTimeout: PT15S
                      headers:
                        X-Namespace: "{{ flow.namespace }}"

                triggers:
                  - id: failed_prod_workflows
                    type: io.kestra.plugin.core.trigger.Flow
                    conditions:
                      - type: io.kestra.plugin.core.condition.ExecutionStatus
                        in:
                          - FAILED
                          - WARNING
                      - type: io.kestra.plugin.core.condition.ExecutionNamespace
                        namespace: prod
                        prefix: true"""
        )
    },
    aliases = "io.kestra.plugin.notifications.sentry.SentryExecution"
)
public class SentryExecution extends SentryTemplate implements ExecutionInterface {
    @Schema(
        title = "Execution ID",
        description = "Defaults to current execution; override to reference another execution."
    )
    @Builder.Default
    private final Property<String> executionId = Property.ofExpression("{{ execution.id }}");

    @Schema(
        title = "Custom fields",
        description = "Extra key/value pairs exposed to the template render map."
    )
    private Property<Map<String, Object>> customFields;

    @Schema(
        title = "Custom message",
        description = "Optional message string injected into the template context."
    )
    private Property<String> customMessage;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        this.templateUri = Property.ofValue("sentry-template.peb");
        this.templateRenderMap = Property.ofValue(ExecutionService.executionMap(runContext, this));

        return super.run(runContext);
    }
}
