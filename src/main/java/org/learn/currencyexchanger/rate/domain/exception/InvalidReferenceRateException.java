package org.learn.currencyexchanger.rate.domain.exception;

public final class InvalidReferenceRateException
        extends IllegalArgumentException {

    private static final String DEFAULT_MESSAGE =
            "Reference rate must be greater than zero";

    public InvalidReferenceRateException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidReferenceRateException(
            String message
    ) {
        super(message);
    }
}
