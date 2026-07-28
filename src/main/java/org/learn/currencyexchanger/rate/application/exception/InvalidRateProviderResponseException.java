package org.learn.currencyexchanger.rate.application.exception;

public final class InvalidRateProviderResponseException
        extends RuntimeException {

    private static final String MESSAGE =
            "Reference rate provider returned an invalid response";

    public InvalidRateProviderResponseException(
            String reason
    ) {
        super(MESSAGE + ": " + reason);
    }

    public InvalidRateProviderResponseException(
            Throwable cause
    ) {
        super(MESSAGE, cause);
    }
}
