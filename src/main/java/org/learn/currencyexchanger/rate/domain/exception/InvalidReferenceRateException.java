package org.learn.currencyexchanger.rate.domain.exception;

public final class InvalidReferenceRateException
        extends IllegalArgumentException {

    private static final String MESSAGE =
            "Reference rate must be greater than zero";

    public InvalidReferenceRateException() {
        super(MESSAGE);
    }
}
