package org.learn.currencyexchanger.auth.infrastructure.persistence;

import org.learn.currencyexchanger.auth.application.port.AuthenticationAttemptRepository;
import org.learn.currencyexchanger.auth.application.port.RecordAuthenticationFailureCommand;
import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptState;
import org.learn.currencyexchanger.auth.domain.AuthenticationSubjectKey;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcAuthenticationAttemptRepository
        implements AuthenticationAttemptRepository {

    private static final String FIND_BY_SUBJECT_KEY = """
            SELECT
                failure_count,
                window_started_at,
                blocked_until
            FROM authentication_attempt
            WHERE subject_key = :subjectKey
            """;

    private static final String RECORD_FAILURE = """
            INSERT INTO authentication_attempt AS stored_attempt (
                subject_key,
                failure_count,
                window_started_at,
                blocked_until,
                updated_at
            )
            VALUES (
                :subjectKey,
                1,
                :occurredAt,
                CASE
                    WHEN :maximumFailures = 1 THEN :blockedUntil
                    ELSE NULL
                END,
                :occurredAt
            )
            ON CONFLICT (subject_key)
            DO UPDATE SET
                failure_count = CASE
                    WHEN stored_attempt.blocked_until > :occurredAt
                        THEN stored_attempt.failure_count
                    WHEN stored_attempt.blocked_until IS NOT NULL
                        OR stored_attempt.window_started_at <= :windowCutoff
                        THEN 1
                    ELSE stored_attempt.failure_count + 1
                END,
                window_started_at = CASE
                    WHEN stored_attempt.blocked_until > :occurredAt
                        THEN stored_attempt.window_started_at
                    WHEN stored_attempt.blocked_until IS NOT NULL
                        OR stored_attempt.window_started_at <= :windowCutoff
                        THEN :occurredAt
                    ELSE stored_attempt.window_started_at
                END,
                blocked_until = CASE
                    WHEN stored_attempt.blocked_until > :occurredAt
                        THEN stored_attempt.blocked_until
                    WHEN (
                        CASE
                            WHEN stored_attempt.blocked_until IS NOT NULL
                                OR stored_attempt.window_started_at <= :windowCutoff
                                THEN 1
                            ELSE stored_attempt.failure_count + 1
                        END
                    ) >= :maximumFailures
                        THEN :blockedUntil
                    ELSE NULL
                END,
                updated_at = :occurredAt
            """;

    private static final String DELETE_BY_SUBJECT_KEY = """
            DELETE FROM authentication_attempt
            WHERE subject_key = :subjectKey
            """;

    private static final String DELETE_UPDATED_BEFORE = """
            DELETE FROM authentication_attempt
            WHERE updated_at < :cutoff
            """;

    private final JdbcClient jdbcClient;

    public JdbcAuthenticationAttemptRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(
                jdbcClient,
                "JDBC client must not be null"
        );
    }

    @Override
    public Optional<AuthenticationAttemptState> findBySubjectKey(
            AuthenticationSubjectKey subjectKey
    ) {
        Objects.requireNonNull(subjectKey, "Subject key must not be null");

        return jdbcClient.sql(FIND_BY_SUBJECT_KEY)
                .param("subjectKey", subjectKey.value())
                .query(JdbcAuthenticationAttemptRepository::mapState)
                .optional();
    }

    @Override
    public void recordFailure(RecordAuthenticationFailureCommand command) {
        Objects.requireNonNull(command, "Failure command must not be null");

        int affectedRows = jdbcClient.sql(RECORD_FAILURE)
                .param("subjectKey", command.subjectKey().value())
                .param("occurredAt", toUtc(command.occurredAt()))
                .param("windowCutoff", toUtc(command.windowCutoff()))
                .param("blockedUntil", toUtc(command.blockedUntil()))
                .param("maximumFailures", command.maximumFailures())
                .update();

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "Recording an authentication failure affected an unexpected number of rows: "
                            + affectedRows
            );
        }
    }

    @Override
    public void deleteBySubjectKey(AuthenticationSubjectKey subjectKey) {
        Objects.requireNonNull(subjectKey, "Subject key must not be null");

        jdbcClient.sql(DELETE_BY_SUBJECT_KEY)
                .param("subjectKey", subjectKey.value())
                .update();
    }

    @Override
    public int deleteUpdatedBefore(Instant cutoff) {
        Objects.requireNonNull(cutoff, "Cleanup cutoff must not be null");

        return jdbcClient.sql(DELETE_UPDATED_BEFORE)
                .param("cutoff", toUtc(cutoff))
                .update();
    }

    private static AuthenticationAttemptState mapState(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        OffsetDateTime blockedUntil = resultSet.getObject(
                "blocked_until",
                OffsetDateTime.class
        );

        return new AuthenticationAttemptState(
                resultSet.getInt("failure_count"),
                resultSet.getObject(
                        "window_started_at",
                        OffsetDateTime.class
                ).toInstant(),
                Optional.ofNullable(blockedUntil)
                        .map(OffsetDateTime::toInstant)
        );
    }

    private static OffsetDateTime toUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
