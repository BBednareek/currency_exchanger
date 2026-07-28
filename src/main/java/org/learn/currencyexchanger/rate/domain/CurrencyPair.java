package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;

import java.util.Objects;

public record CurrencyPair(
        CurrencyCode base,
        CurrencyCode quote
) {

    public CurrencyPair {
        Objects.requireNonNull(
                base,
                "Base currency cannot be null"
        );

        Objects.requireNonNull(
                quote,
                "Quote currency cannot be null"
        );

        if (base.equals(quote)) {
            throw new InvalidCurrencyPairException();
        }
    }

    public static CurrencyPair of(
            String base,
            String quote
    ) {
        return new CurrencyPair(
                new CurrencyCode(base),
                new CurrencyCode(quote)
        );
    }

    public String symbol() {
        return "%s/%s".formatted(base, quote);
    }

    @Override
    public String toString() {
        return symbol();
    }
}
