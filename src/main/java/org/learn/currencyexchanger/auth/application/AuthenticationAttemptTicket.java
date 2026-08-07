package org.learn.currencyexchanger.auth.application;

import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;

import java.util.Objects;

public record AuthenticationAttemptTicket(
        AuthenticationSubjectKey subjectKey
) {

    public AuthenticationAttemptTicket {
        Objects.requireNonNull(
                subjectKey,
                "Authentication subject key must not be null"
        );
    }
}
