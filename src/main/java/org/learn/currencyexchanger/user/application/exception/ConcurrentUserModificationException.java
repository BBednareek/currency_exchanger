package org.learn.currencyexchanger.user.application.exception;

import java.util.Objects;
import java.util.UUID;

public final class ConcurrentUserModificationException
        extends RuntimeException {
    public ConcurrentUserModificationException(
            UUID userId,
            Throwable cause
    ) {
        super(
                "User was modified concurrently: "
                        + Objects.requireNonNull(
                        userId,
                        "User ID must not be null"
                ),
                Objects.requireNonNull(
                        cause,
                        "Cause must not be null"
                )
        );
    }
}
