package org.learn.currencyexchanger.auth.domain;

import java.time.Duration;
import java.util.Objects;

public record AuthenticationAttemptPolicy(
        int maximumFailures,
        Duration failureWindow,
        Duration blockDuration,
        Duration retention
) {

    public AuthenticationAttemptPolicy {
        if (maximumFailures < 1) {
            throw new IllegalArgumentException(
                    "Maximum authentication failures must be positive"
            );
        }

        failureWindow = requirePositive(
                failureWindow,
                "Failure window"
        );
        blockDuration = requirePositive(
                blockDuration,
                "Block duration"
        );
        retention = requirePositive(retention, "Retention");

        Duration minimumRetention = failureWindow.compareTo(blockDuration) >= 0
                ? failureWindow
                : blockDuration;

        if (retention.compareTo(minimumRetention) < 0) {
            throw new IllegalArgumentException(
                    "Retention must not be shorter than the failure window "
                            + "or block duration"
            );
        }
    }

    private static Duration requirePositive(
            Duration duration,
            String propertyName
    ) {
        Objects.requireNonNull(
                duration,
                propertyName + " must not be null"
        );

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    propertyName + " must be positive"
            );
        }

        return duration;
    }
}
