package org.learn.currencyexchanger.auth.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthenticationAttemptConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            AuthenticationAttemptConfiguration.class
                    );

    @Test
    void shouldBindValidAuthenticationAttemptPolicy() {
        contextRunner
                .withPropertyValues(
                        "security.authentication-attempts.maximum-failures=5",
                        "security.authentication-attempts.failure-window=15m",
                        "security.authentication-attempts.block-duration=30m",
                        "security.authentication-attempts.retention=168h",
                        "security.authentication-attempts.cleanup-interval=1h"
                )
                .run(context -> {
                    assertNull(context.getStartupFailure());

                    AuthenticationAttemptProperties properties =
                            context.getBean(
                                    AuthenticationAttemptProperties.class
                            );

                    assertAll(
                            () -> assertEquals(
                                    5,
                                    properties.maximumFailures()
                            ),
                            () -> assertEquals(
                                    Duration.ofMinutes(15),
                                    properties.failureWindow()
                            ),
                            () -> assertEquals(
                                    Duration.ofMinutes(30),
                                    properties.blockDuration()
                            ),
                            () -> assertEquals(
                                    Duration.ofDays(7),
                                    properties.retention()
                            ),
                            () -> assertEquals(
                                    Duration.ofHours(1),
                                    properties.cleanupInterval()
                            )
                    );
                });
    }

    @Test
    void shouldRejectRetentionShorterThanTheFailureWindow() {
        contextRunner
                .withPropertyValues(
                        "security.authentication-attempts.maximum-failures=5",
                        "security.authentication-attempts.failure-window=2h",
                        "security.authentication-attempts.block-duration=15m",
                        "security.authentication-attempts.retention=1h",
                        "security.authentication-attempts.cleanup-interval=1h"
                )
                .run(context ->
                        assertNotNull(context.getStartupFailure())
                );
    }
}
