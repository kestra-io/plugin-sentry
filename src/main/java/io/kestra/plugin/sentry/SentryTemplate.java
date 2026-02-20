package io.kestra.plugin.sentry;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class SentryTemplate extends SentryAlert {

    @Schema(
        title = "Template resource path",
        hidden = true
    )
    protected Property<String> templateUri;

    @Schema(
        title = "Template render variables",
        description = "Key/value map rendered into the template before sending."
    )
    protected Property<Map<String, Object>> templateRenderMap;

    @Schema(
        title = "Event identifier",
        description = "Lowercase uuid4 without dashes; auto-generated when omitted.",
        defaultValue = "a generated unique identifier"
    )
    @Pattern(regexp = "[0-9a-f]{8}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{12}")
    @NotNull
    @Builder.Default
    @PluginProperty(dynamic = true)
    protected String eventId = UUID.randomUUID().toString().toLowerCase().replace("-", "");

    @Schema(
        title = "Sentry platform",
        description = "Defaults to JAVA; Sentry uses it to adapt parsing and UI."
    )
    @NotNull
    @Builder.Default
    protected Property<Platform> platform = Property.ofValue(Platform.JAVA);

    @Schema(
        title = "Alert severity level",
        description = "Accepts fatal, error, warning, info, debug; default is ERROR."
    )
    @Builder.Default
    protected Property<ErrorLevel> level = Property.ofValue(ErrorLevel.ERROR);

    @Schema(
        title = "Transaction name",
        description = "Free-form route or operation name attached to the event."
    )
    protected Property<String> transaction;

    @Schema(
        title = "Server name",
        description = "Host identifier reported with the event."
    )
    protected Property<String> serverName;

    @Schema(
        title = "Extra metadata",
        description = "Merged into `extra` payload object after template rendering."
    )
    protected Property<Map<String, Object>> extra;

    @Schema(
        title = "Error details",
        description = "Optional `errors` payload section; overrides template values."
    )
    protected Property<Map<String, Object>> errors;

    @SuppressWarnings("unchecked")
    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        Map<String, Object> map = new HashMap<>();

        final var renderedTemplateUri = runContext.render(this.templateUri).as(String.class);
        if (renderedTemplateUri.isPresent()) {
            String template = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(renderedTemplateUri.get())),
                StandardCharsets.UTF_8
                                              );

            String render = runContext.render(template, templateRenderMap != null ?
                runContext.render(templateRenderMap).asMap(String.class, Object.class) :
                Map.of()
            );
            map = (Map<String, Object>) JacksonMapper.ofJson().readValue(render, Object.class);
        }

        map.put("event_id", eventId);
        map.put("timestamp", Instant.now().toString());
        map.put("platform", runContext.render(platform).as(Platform.class).get().name().toLowerCase());

        if (runContext.render(this.level).as(ErrorLevel.class).isPresent()) {
            map.put("level", runContext.render(this.level).as(ErrorLevel.class).get().name().toLowerCase());
        }

        if (runContext.render(this.transaction).as(String.class).isPresent()) {
            map.put("transaction", runContext.render(this.transaction).as(String.class).get());
        }

        if (runContext.render(this.serverName).as(String.class).isPresent()) {
            map.put("server_name", runContext.render(this.serverName).as(String.class).get());
        }

        final Map<String, Object> renderedExtraMap = runContext.render(extra).asMap(String.class, Object.class);
        if (!renderedExtraMap.isEmpty()) {
            Map<String, Object> extra = (Map) map.getOrDefault("extra", new HashMap<>());
            extra.putAll(renderedExtraMap);
            map.put("extra", extra);
        }

        final Map<String, Object> renderedErrorsMap = runContext.render(this.errors).asMap(String.class, Object.class);
        if (!renderedErrorsMap.isEmpty()) {
            map.put("errors", renderedErrorsMap);
        }

        this.payload = Property.ofValue(JacksonMapper.ofJson().writeValueAsString(map));

        return super.run(runContext);
    }

}
