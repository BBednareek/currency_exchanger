package org.learn.currencyexchanger.auth.application.port;

import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptState;
import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;

import java.time.Instant;
import java.util.Optional;

public interface AuthenticationAttemptRepository {

    Optional<AuthenticationAttemptState> findBySubjectKey(
            AuthenticationSubjectKey subjectKey
    );

    void recordFailure(RecordAuthenticationFailureCommand command);

    void deleteBySubjectKey(AuthenticationSubjectKey subjectKey);

    int deleteUpdatedBefore(Instant cutoff);
}
