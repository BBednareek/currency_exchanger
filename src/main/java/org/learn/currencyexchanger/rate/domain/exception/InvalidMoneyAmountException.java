package org.learn.currencyexchanger.rate.domain.exception;

public final class InvalidMoneyAmountException
        extends IllegalArgumentException {

    private static final String MESSAGE =
            "Money amount must be greater than zero";

    public InvalidMoneyAmountException() {
        super(MESSAGE);
    }
}
