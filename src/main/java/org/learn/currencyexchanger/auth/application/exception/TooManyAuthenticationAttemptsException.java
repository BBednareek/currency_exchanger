package org.learn.currencyexchanger.auth.application.exception;

import java.time.Duration;
import java.util.Objects;

public final class TooManyAuthenticationAttemptsException
        extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAuthenticationAttemptsException(Duration retryAfter) {
        super("Too many authentication attempts. Try again later.");

        this.retryAfter = requirePositive(retryAfter);
    }

    private static Duration requirePositive(Duration retryAfter) {
        Objects.requireNonNull(retryAfter, "retryAfter must not be null");

        if (retryAfter.isZero() || retryAfter.isNegative()) {
            throw new IllegalArgumentException(
                    "retryAfter must be positive"
            );
        }

        return retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    public long retryAfterSeconds() {
        long seconds = retryAfter.getSeconds();

        if (retryAfter.getNano() > 0) {
            seconds = Math.addExact(seconds, 1);
        }

        return Math.max(1, seconds);
    }
}
