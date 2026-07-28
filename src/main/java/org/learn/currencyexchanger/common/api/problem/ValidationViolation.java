package org.learn.currencyexchanger.common.api.problem;

import java.util.Objects;

public record ValidationViolation(
        String path,
        String message
) {

    public ValidationViolation {
        Objects.requireNonNull(path, "Validation path must not be null");
        Objects.requireNonNull(message, "Validation message must not be null");
    }
}
