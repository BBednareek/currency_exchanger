package org.learn.currencyexchanger.rate.application.exception;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;

public final class UnsupportedCurrencyException
        extends RuntimeException {

    public UnsupportedCurrencyException(
            CurrencyPair pair
    ) {
        super(
                "Currency pair is not supported: "
                        + pair.symbol()
        );
    }
}
