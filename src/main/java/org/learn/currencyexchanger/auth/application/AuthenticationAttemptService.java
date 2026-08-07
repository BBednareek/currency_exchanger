package org.learn.currencyexchanger.auth.application;

import org.learn.currencyexchanger.auth.application.exception.TooManyAuthenticationAttemptsException;
import org.learn.currencyexchanger.auth.application.port.AuthenticationAttemptRepository;
import org.learn.currencyexchanger.auth.application.port.RecordAuthenticationFailureCommand;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptPolicy;
import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AuthenticationAttemptService {

    private final AuthenticationAttemptRepository repository;
    private final AuthenticationAttemptPolicy policy;
    private final Clock clock;

    public AuthenticationAttemptService(
            AuthenticationAttemptRepository repository,
            AuthenticationAttemptPolicy policy,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Authentication attempt repository must not be null"
        );
        this.policy = Objects.requireNonNull(
                policy,
                "Authentication attempt policy must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    public AuthenticationAttemptTicket beginAttempt(String rawUsername) {
        AuthenticationSubjectKey subjectKey =
                AuthenticationSubjectKey.fromUsername(rawUsername);

        Instant now = clock.instant();

        repository.findBySubjectKey(subjectKey)
                .flatMap(state -> state.remainingBlockAt(now))
                .ifPresent(remainingBlock -> {
                    throw new TooManyAuthenticationAttemptsException(
                            remainingBlock
                    );
                });

        return new AuthenticationAttemptTicket(subjectKey);
    }

    @Transactional
    public void recordFailure(AuthenticationAttemptTicket ticket) {
        Objects.requireNonNull(
                ticket,
                "Authentication attempt ticket must not be null"
        );

        Instant occurredAt = clock.instant();

        repository.recordFailure(
                new RecordAuthenticationFailureCommand(
                        ticket.subjectKey(),
                        occurredAt,
                        occurredAt.minus(policy.failureWindow()),
                        occurredAt.plus(policy.blockDuration()),
                        policy.maximumFailures()
                )
        );
    }

    @Transactional
    public void recordSuccess(AuthenticationAttemptTicket ticket) {
        Objects.requireNonNull(
                ticket,
                "Authentication attempt ticket must not be null"
        );

        repository.deleteBySubjectKey(ticket.subjectKey());
    }

    @Transactional
    public int deleteExpiredAttempts() {
        Instant cutoff = clock.instant().minus(policy.retention());

        return repository.deleteUpdatedBefore(cutoff);
    }
}
