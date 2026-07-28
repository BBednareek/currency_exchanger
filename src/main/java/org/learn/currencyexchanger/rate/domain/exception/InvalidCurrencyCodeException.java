package org.learn.currencyexchanger.rate.domain.exception;

public final class InvalidCurrencyCodeException
        extends IllegalArgumentException {

    private static final String MESSAGE =
            "Currency code must contain exactly three ASCII letters";

    public InvalidCurrencyCodeException() {
        super(MESSAGE);
    }
}
