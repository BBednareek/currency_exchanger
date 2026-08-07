package org.learn.currencyexchanger.auth.application.port;

import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;

import java.time.Instant;
import java.util.Objects;

public record RecordAuthenticationFailureCommand(
        AuthenticationSubjectKey subjectKey,
        Instant occurredAt,
        Instant windowCutoff,
        Instant blockedUntil,
        int maximumFailures
) {

    public RecordAuthenticationFailureCommand {
        Objects.requireNonNull(subjectKey, "Subject key must not be null");
        Objects.requireNonNull(occurredAt, "Occurrence time must not be null");
        Objects.requireNonNull(windowCutoff, "Window cutoff must not be null");
        Objects.requireNonNull(blockedUntil, "Block end must not be null");

        if (!windowCutoff.isBefore(occurredAt)) {
            throw new IllegalArgumentException(
                    "Window cutoff must be before the occurrence time"
            );
        }

        if (!blockedUntil.isAfter(occurredAt)) {
            throw new IllegalArgumentException(
                    "Block end must be after the occurrence time"
            );
        }

        if (maximumFailures < 1) {
            throw new IllegalArgumentException(
                    "Maximum authentication failures must be positive"
            );
        }
    }
}
