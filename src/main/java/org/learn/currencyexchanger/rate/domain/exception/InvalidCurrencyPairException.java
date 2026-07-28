package org.learn.currencyexchanger.rate.domain.exception;

public final class InvalidCurrencyPairException
        extends IllegalArgumentException {

    private static final String MESSAGE =
            "Base and quote currencies must be different";

    public InvalidCurrencyPairException() {
        super(MESSAGE);
    }
}
