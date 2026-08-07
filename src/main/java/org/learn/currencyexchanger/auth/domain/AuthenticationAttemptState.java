package org.learn.currencyexchanger.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AuthenticationAttemptState(
        int failureCount,
        Instant windowStartedAt,
        Optional<Instant> blockedUntil
) {

    public AuthenticationAttemptState {
        if (failureCount < 1) {
            throw new IllegalArgumentException(
                    "Authentication failure count must be positive"
            );
        }

        Objects.requireNonNull(
                windowStartedAt,
                "Authentication attempt window start must not be null"
        );

        blockedUntil = Objects.requireNonNull(
                blockedUntil,
                "Blocked-until container must not be null"
        );

        blockedUntil.ifPresent(value -> {
            if (!value.isAfter(windowStartedAt)) {
                throw new IllegalArgumentException(
                        "Blocked-until timestamp must be after the window start"
                );
            }
        });
    }

    public Optional<Duration> remainingBlockAt(Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");

        return blockedUntil
                .filter(value -> value.isAfter(instant))
                .map(value -> Duration.between(instant, value));
    }
}
