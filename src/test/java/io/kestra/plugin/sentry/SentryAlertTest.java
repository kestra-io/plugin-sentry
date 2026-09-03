package io.kestra.plugin.sentry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KestraTest
public class SentryAlertTest {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @DisplayName("Run with deprecated /store endpoint")
    void runWithStoreEndpoint() throws Exception {
        RunContext runContext = runContextFactory.of(
            Map.of(
                "extra", Map.of(
                    "title", "Sentry test alert notification to store endpoint",
                    "text", "ge *with some bold text* an",
                    "service", IdUtils.create()
                )
            )
        );

        EmbeddedServer embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();

        SentryAlert task = SentryAlert.builder()
            .id(IdUtils.create())
            .dsn(embeddedServer.getURI() + "/webhook-unit-test")
            .endpointType(EndpointType.STORE)
            .payload(
                Property.ofExpression(
                    Files.asCharSource(
                        new File(
                            Objects.requireNonNull(
                                SentryAlertTest.class.getClassLoader()
                                    .getResource("sentry.peb")
                            )
                                .toURI()
                        ),
                        StandardCharsets.UTF_8
                    ).read()
                )
            )
            .build();

        task.run(runContext);

        assertAll(
            "Grouped Assertions of Store Data",
            () -> assertThat(FakeWebhookController.data, containsString("Sentry test alert notification to store endpoint")),
            () -> assertThat(FakeWebhookController.data, containsString("ge *with some bold text* an"))
        );
    }

    @Test
    @DisplayName("Run with new /envelope endpoint")
    void runWithEnvelopeEndpoint() throws Exception {
        RunContext runContext = runContextFactory.of(
            Map.of(
                "eventId", "c91832d35bd54bbebc20f3e6b8e84538", "extra", Map.of(
                    "title", "Sentry test alert notification to envelope endpoint",
                    "text", "ge *with some bold text* an",
                    "service", IdUtils.create()
                )
            )
        );

        EmbeddedServer embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();

        SentryAlert task = SentryAlert.builder()
            .id(IdUtils.create())
            .dsn(embeddedServer.getURI() + "/webhook-unit-test")
            .endpointType(EndpointType.ENVELOPE)
            .payload(
                Property.ofExpression(
                    Files.asCharSource(
                        new File(
                            Objects.requireNonNull(
                                SentryAlertTest.class.getClassLoader()
                                    .getResource("sentry.peb")
                            )
                                .toURI()
                        ),
                        StandardCharsets.UTF_8
                    ).read()
                )
            )
            .build();

        task.run(runContext);

        assertAll(
            "Grouped Assertions of Envelope Data",
            () -> assertThat(FakeWebhookController.data, containsString("c91832d35bd54bbebc20f3e6b8e84538")),
            () -> assertThat(FakeWebhookController.data, containsString("Sentry test alert notification to envelope endpoint")),
            () -> assertThat(FakeWebhookController.data, containsString("ge *with some bold text* an"))
        );
    }

    @Test
    @DisplayName("Task fails when Sentry rejects the payload (400)")
    void runFailsOnBadRequest() throws Exception {
        assertThrows(HttpClientResponseException.class, () -> runAgainstErrorEndpoint(400));
    }

    @Test
    @DisplayName("Task fails when Sentry rate-limits the request (429)")
    void runFailsOnRateLimit() throws Exception {
        assertThrows(HttpClientResponseException.class, () -> runAgainstErrorEndpoint(429));
    }

    @Test
    @DisplayName("Task fails and logs the store-endpoint hint on 401/404 with the envelope endpoint")
    void runFailsAndLogsHintOnEnvelopeAuthError() throws Exception {
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("unit-test");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);

        try {
            assertThrows(HttpClientResponseException.class, () -> runAgainstErrorEndpoint(401));

            List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertTrue(
                messages.stream().anyMatch(message -> message.contains("configure the store endpoint")),
                "Expected a log hint pointing users to endpointType: store, got: " + messages
            );
        } finally {
            logbackLogger.detachAppender(appender);
        }
    }

    private void runAgainstErrorEndpoint(int status) throws Exception {
        RunContext runContext = runContextFactory.of(
            Map.of(
                "extra", Map.of(
                    "title", "Sentry test alert notification to error endpoint",
                    "text", "ge *with some bold text* an",
                    "service", IdUtils.create()
                )
            )
        );

        EmbeddedServer embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();

        SentryAlert task = SentryAlert.builder()
            .id(IdUtils.create())
            .dsn(embeddedServer.getURI() + "/webhook-unit-test/error/" + status)
            .endpointType(EndpointType.ENVELOPE)
            .payload(
                Property.ofExpression(
                    Files.asCharSource(
                        new File(
                            Objects.requireNonNull(
                                SentryAlertTest.class.getClassLoader()
                                    .getResource("sentry.peb")
                            )
                                .toURI()
                        ),
                        StandardCharsets.UTF_8
                    ).read()
                )
            )
            .build();

        task.run(runContext);
    }
}
