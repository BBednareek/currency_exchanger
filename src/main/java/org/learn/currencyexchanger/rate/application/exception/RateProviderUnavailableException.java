package org.learn.currencyexchanger.rate.application.exception;

public final class RateProviderUnavailableException
        extends RuntimeException {

    private static final String MESSAGE =
            "Reference rate provider is unavailable";

    public RateProviderUnavailableException() {
        super(MESSAGE);
    }

    public RateProviderUnavailableException(
            Throwable cause
    ) {
        super(MESSAGE, cause);
    }
}
