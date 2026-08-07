package org.learn.currencyexchanger.auth.infrastructure.maintenance;

import org.learn.currencyexchanger.auth.application.AuthenticationAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class AuthenticationAttemptCleanupJob {

    private static final String CLEANUP_INTERVAL_PROPERTY =
            "${security.authentication-attempts.cleanup-interval}";

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AuthenticationAttemptCleanupJob.class
    );

    private final AuthenticationAttemptService authenticationAttemptService;

    public AuthenticationAttemptCleanupJob(
            AuthenticationAttemptService authenticationAttemptService
    ) {
        this.authenticationAttemptService = authenticationAttemptService;
    }

    @Scheduled(
            initialDelayString = CLEANUP_INTERVAL_PROPERTY,
            fixedDelayString = CLEANUP_INTERVAL_PROPERTY
    )
    public void removeExpiredAttempts() {
        int removedAttempts =
                authenticationAttemptService.deleteExpiredAttempts();

        if (removedAttempts > 0) {
            LOGGER.debug(
                    "Removed {} expired authentication attempt records",
                    removedAttempts
            );
        }
    }
}
