package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(
        CurrencyCode currency,
        BigDecimal amount
) {

    public Money {
        Objects.requireNonNull(
                currency,
                "Money currency cannot be null"
        );

        Objects.requireNonNull(
                amount,
                "Money amount cannot be null"
        );

        if (amount.signum() <= 0) {
            throw new InvalidMoneyAmountException();
        }
    }

    public static Money of(
            String currency,
            BigDecimal amount
    ) {
        return new Money(
                new CurrencyCode(currency),
                amount
        );
    }
}
