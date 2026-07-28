package org.learn.currencyexchanger.rate.application.exception;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;

public final class ReferenceRateNotFoundException
        extends RuntimeException {

    public ReferenceRateNotFoundException(
            CurrencyPair pair
    ) {
        super(
                "Reference rate was not found for: "
                        + pair.symbol()
        );
    }
}
