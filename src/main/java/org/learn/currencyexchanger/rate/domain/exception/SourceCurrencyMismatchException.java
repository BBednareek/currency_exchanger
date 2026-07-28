package org.learn.currencyexchanger.rate.domain.exception;

import org.learn.currencyexchanger.rate.domain.CurrencyCode;

public final class SourceCurrencyMismatchException
        extends IllegalArgumentException {
    public SourceCurrencyMismatchException(
            CurrencyCode actual,
            CurrencyCode expected
    ) {
        super(
                "Source amount currency %s does not match reference rate base currency %s"
                        .formatted(actual, expected)
        );
    }
}
