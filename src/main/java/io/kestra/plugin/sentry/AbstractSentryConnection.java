package io.kestra.plugin.sentry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.client.configurations.TimeoutConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSentryConnection extends Task implements RunnableTask<VoidOutput> {
    @Schema(
        title = "HTTP client options",
        description = "Tune Sentry request timeouts, charset, and headers; defaults suit most flows."
    )
    @PluginProperty(dynamic = true, group = "advanced")
    protected RequestOptions options;

    protected HttpConfiguration httpClientConfigurationWithOptions() throws IllegalVariableEvaluationException {
        HttpConfiguration.HttpConfigurationBuilder configuration = HttpConfiguration.builder();

        if (this.options != null) {

            configuration
                .timeout(
                    TimeoutConfiguration.builder()
                        .connectTimeout(this.options.getConnectTimeout())
                        .readIdleTimeout(this.options.getReadIdleTimeout())
                        .build()
                )
                .defaultCharset(this.options.getDefaultCharset());
        }

        return configuration.build();
    }

    protected HttpRequest.HttpRequestBuilder createRequestBuilder(
        RunContext runContext) throws IllegalVariableEvaluationException {

        HttpRequest.HttpRequestBuilder builder = HttpRequest.builder();

        if (this.options != null && this.options.getHeaders() != null) {
            Map<String, String> headers = runContext.render(this.options.getHeaders())
                .asMap(String.class, String.class);

            if (headers != null) {
                headers.forEach(builder::addHeader);
            }
        }
        return builder;
    }

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(
            title = "Connect timeout",
            description = "Optional socket connect timeout; if unset, HTTP client default applies."
        )
        @PluginProperty(group = "execution")
        private final Property<Duration> connectTimeout;

        @Deprecated
        @Schema(
            title = "Read timeout",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client has no separate read timeout. Use `readIdleTimeout` instead.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Duration> readTimeout = Property.ofValue(Duration.ofSeconds(10));

        @Schema(
            title = "Read idle timeout",
            description = "Closes idle read connections after inactivity; defaults to 5 minutes."
        )
        @Builder.Default
        @PluginProperty(group = "execution")
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Deprecated
        @Schema(
            title = "Connection pool idle timeout",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client does not support a connection pool idle timeout.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Deprecated
        @Schema(
            title = "Maximum response size",
            description = """
                Deprecated, this option has no effect: Kestra's HTTP client does not limit the response size.
                """
        )
        @Builder.Default
        @PluginProperty(group = "deprecated")
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(
            title = "Default request charset",
            description = "Charset used for requests when none is specified; defaults to UTF-8."
        )
        @Builder.Default
        @PluginProperty(group = "advanced")
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);

        @Schema(
            title = "HTTP headers",
            description = "Rendered header map applied to the outbound request."
        )
        @PluginProperty(group = "advanced")
        public Property<Map<String, String>> headers;
    }
}
