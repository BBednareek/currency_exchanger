package org.learn.currencyexchanger.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.auth.application.exception.TooManyAuthenticationAttemptsException;
import org.learn.currencyexchanger.auth.application.port.AuthenticationAttemptRepository;
import org.learn.currencyexchanger.auth.application.port.RecordAuthenticationFailureCommand;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptPolicy;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptState;
import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationAttemptServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T10:00:00Z");

    private RecordingAuthenticationAttemptRepository repository;
    private AuthenticationAttemptService service;

    @BeforeEach
    void setUp() {
        repository = new RecordingAuthenticationAttemptRepository();

        AuthenticationAttemptPolicy policy =
                new AuthenticationAttemptPolicy(
                        5,
                        Duration.ofMinutes(15),
                        Duration.ofMinutes(30),
                        Duration.ofDays(7)
                );

        service = new AuthenticationAttemptService(
                repository,
                policy,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateTicketWhenSubjectIsNotBlocked() {
        AuthenticationAttemptTicket ticket =
                service.beginAttempt("  JOHN.DOE  ");

        assertEquals(
                AuthenticationSubjectKey.fromUsername("john.doe"),
                ticket.subjectKey()
        );
    }

    @Test
    void shouldRejectAttemptWhileSubjectIsBlocked() {
        AuthenticationSubjectKey subjectKey =
                AuthenticationSubjectKey.fromUsername("john.doe");

        repository.state = Optional.of(
                new AuthenticationAttemptState(
                        5,
                        NOW.minus(Duration.ofMinutes(5)),
                        Optional.of(NOW.plus(Duration.ofSeconds(45)))
                )
        );

        TooManyAuthenticationAttemptsException exception =
                assertThrows(
                        TooManyAuthenticationAttemptsException.class,
                        () -> service.beginAttempt("john.doe")
                );

        assertAll(
                () -> assertEquals(
                        Duration.ofSeconds(45),
                        exception.retryAfter()
                ),
                () -> assertEquals(
                        subjectKey,
                        repository.lastLookupKey
                )
        );
    }

    @Test
    void shouldRecordFailureUsingConfiguredPolicy() {
        AuthenticationAttemptTicket ticket =
                service.beginAttempt("john.doe");

        service.recordFailure(ticket);

        RecordAuthenticationFailureCommand command =
                repository.lastFailure;

        assertAll(
                () -> assertEquals(ticket.subjectKey(), command.subjectKey()),
                () -> assertEquals(NOW, command.occurredAt()),
                () -> assertEquals(
                        NOW.minus(Duration.ofMinutes(15)),
                        command.windowCutoff()
                ),
                () -> assertEquals(
                        NOW.plus(Duration.ofMinutes(30)),
                        command.blockedUntil()
                ),
                () -> assertEquals(5, command.maximumFailures())
        );
    }

    @Test
    void shouldRemoveFailureStateAfterSuccessfulAuthentication() {
        AuthenticationAttemptTicket ticket =
                service.beginAttempt("john.doe");

        service.recordSuccess(ticket);

        assertEquals(ticket.subjectKey(), repository.deletedSubjectKey);
    }

    @Test
    void shouldDeleteAttemptsOlderThanRetention() {
        repository.deletedExpiredCount = 3;

        int deletedCount = service.deleteExpiredAttempts();

        assertAll(
                () -> assertEquals(3, deletedCount),
                () -> assertEquals(
                        NOW.minus(Duration.ofDays(7)),
                        repository.lastCleanupCutoff
                )
        );
    }

    private static final class RecordingAuthenticationAttemptRepository
            implements AuthenticationAttemptRepository {

        private Optional<AuthenticationAttemptState> state = Optional.empty();
        private AuthenticationSubjectKey lastLookupKey;
        private RecordAuthenticationFailureCommand lastFailure;
        private AuthenticationSubjectKey deletedSubjectKey;
        private Instant lastCleanupCutoff;
        private int deletedExpiredCount;

        @Override
        public Optional<AuthenticationAttemptState> findBySubjectKey(
                AuthenticationSubjectKey subjectKey
        ) {
            lastLookupKey = subjectKey;
            return state;
        }

        @Override
        public void recordFailure(
                RecordAuthenticationFailureCommand command
        ) {
            lastFailure = command;
        }

        @Override
        public void deleteBySubjectKey(
                AuthenticationSubjectKey subjectKey
        ) {
            deletedSubjectKey = subjectKey;
        }

        @Override
        public int deleteUpdatedBefore(Instant cutoff) {
            lastCleanupCutoff = cutoff;
            return deletedExpiredCount;
        }
    }
}
