package org.learn.currencyexchanger.auth.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.auth.application.port.AuthenticationAttemptRepository;
import org.learn.currencyexchanger.auth.application.port.RecordAuthenticationFailureCommand;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptState;
import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JdbcAuthenticationAttemptRepositoryTest {

    private static final Instant INITIAL_ATTEMPT =
            Instant.parse("2026-08-05T10:00:00Z");

    private static final Duration FAILURE_WINDOW =
            Duration.ofMinutes(15);

    private static final Duration BLOCK_DURATION =
            Duration.ofMinutes(30);

    @Autowired
    private AuthenticationAttemptRepository repository;

    @Test
    void shouldAtomicallyIncrementFailuresAndBlockAtThreshold() {
        AuthenticationSubjectKey subjectKey = uniqueSubjectKey(
                "threshold"
        );

        repository.recordFailure(command(subjectKey, INITIAL_ATTEMPT, 3));
        repository.recordFailure(command(
                subjectKey,
                INITIAL_ATTEMPT.plusSeconds(1),
                3
        ));
        repository.recordFailure(command(
                subjectKey,
                INITIAL_ATTEMPT.plusSeconds(2),
                3
        ));

        AuthenticationAttemptState state = repository
                .findBySubjectKey(subjectKey)
                .orElseThrow();

        assertAll(
                () -> assertEquals(3, state.failureCount()),
                () -> assertEquals(
                        INITIAL_ATTEMPT,
                        state.windowStartedAt()
                ),
                () -> assertEquals(
                        INITIAL_ATTEMPT
                                .plusSeconds(2)
                                .plus(BLOCK_DURATION),
                        state.blockedUntil().orElseThrow()
                )
        );
    }

    @Test
    void shouldNotExtendAnAlreadyActiveBlock() {
        AuthenticationSubjectKey subjectKey = uniqueSubjectKey(
                "active-block"
        );

        repository.recordFailure(command(subjectKey, INITIAL_ATTEMPT, 1));

        Instant originalBlockEnd =
                INITIAL_ATTEMPT.plus(BLOCK_DURATION);

        repository.recordFailure(command(
                subjectKey,
                INITIAL_ATTEMPT.plusSeconds(1),
                1
        ));

        AuthenticationAttemptState state = repository
                .findBySubjectKey(subjectKey)
                .orElseThrow();

        assertAll(
                () -> assertEquals(1, state.failureCount()),
                () -> assertEquals(
                        originalBlockEnd,
                        state.blockedUntil().orElseThrow()
                )
        );
    }

    @Test
    void shouldStartNewWindowWhenPreviousWindowExpired() {
        AuthenticationSubjectKey subjectKey = uniqueSubjectKey(
                "expired-window"
        );

        repository.recordFailure(command(subjectKey, INITIAL_ATTEMPT, 5));

        Instant nextWindowStart =
                INITIAL_ATTEMPT.plus(FAILURE_WINDOW).plusSeconds(1);

        repository.recordFailure(command(subjectKey, nextWindowStart, 5));

        AuthenticationAttemptState state = repository
                .findBySubjectKey(subjectKey)
                .orElseThrow();

        assertAll(
                () -> assertEquals(1, state.failureCount()),
                () -> assertEquals(
                        nextWindowStart,
                        state.windowStartedAt()
                ),
                () -> assertTrue(state.blockedUntil().isEmpty())
        );
    }

    @Test
    void shouldDeleteStateAfterSuccessfulAuthentication() {
        AuthenticationSubjectKey subjectKey = uniqueSubjectKey(
                "success"
        );

        repository.recordFailure(command(subjectKey, INITIAL_ATTEMPT, 5));
        repository.deleteBySubjectKey(subjectKey);

        assertTrue(repository.findBySubjectKey(subjectKey).isEmpty());
    }

    @Test
    void shouldDeleteOnlyExpiredStates() {
        AuthenticationSubjectKey expired = uniqueSubjectKey("expired");
        AuthenticationSubjectKey current = uniqueSubjectKey("current");

        repository.recordFailure(command(expired, INITIAL_ATTEMPT, 5));
        repository.recordFailure(command(
                current,
                INITIAL_ATTEMPT.plus(Duration.ofDays(2)),
                5
        ));

        int deleted = repository.deleteUpdatedBefore(
                INITIAL_ATTEMPT.plus(Duration.ofDays(1))
        );

        assertAll(
                () -> assertEquals(1, deleted),
                () -> assertTrue(
                        repository.findBySubjectKey(expired).isEmpty()
                ),
                () -> assertFalse(
                        repository.findBySubjectKey(current).isEmpty()
                )
        );
    }

    private static RecordAuthenticationFailureCommand command(
            AuthenticationSubjectKey subjectKey,
            Instant occurredAt,
            int maximumFailures
    ) {
        return new RecordAuthenticationFailureCommand(
                subjectKey,
                occurredAt,
                occurredAt.minus(FAILURE_WINDOW),
                occurredAt.plus(BLOCK_DURATION),
                maximumFailures
        );
    }

    private static AuthenticationSubjectKey uniqueSubjectKey(String prefix) {
        return AuthenticationSubjectKey.fromUsername(
                prefix + "-" + java.util.UUID.randomUUID()
        );
    }
}
