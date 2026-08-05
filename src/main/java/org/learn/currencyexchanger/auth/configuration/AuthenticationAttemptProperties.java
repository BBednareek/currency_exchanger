package org.learn.currencyexchanger.auth.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.authentication-attempts")
public record AuthenticationAttemptProperties(
        @Min(1)
        int maximumFailures,

        @NotNull
        Duration failureWindow,

        @NotNull
        Duration blockDuration,

        @NotNull
        Duration retention,

        @NotNull
        Duration cleanupInterval
) {

    public AuthenticationAttemptProperties {
        new AuthenticationAttemptPolicy(
                maximumFailures,
                failureWindow,
                blockDuration,
                retention
        );

        if (cleanupInterval == null
                || cleanupInterval.isZero()
                || cleanupInterval.isNegative()) {
            throw new IllegalArgumentException(
                    "cleanupInterval must be positive"
            );
        }
    }
}
