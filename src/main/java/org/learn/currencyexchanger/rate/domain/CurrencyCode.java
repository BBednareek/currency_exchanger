package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record CurrencyCode(String value) {
    private final static Pattern CURRENCY_CODE_PATTERN =
            Pattern.compile("[A-Z]{3}");


    public CurrencyCode {
        Objects.requireNonNull(
                value,
                "CurrencyCode cannot be null"
        );

        String normalizedValue = value.strip().toUpperCase(Locale.ROOT);

        if (!CURRENCY_CODE_PATTERN.matcher(normalizedValue).matches()) {
            throw new InvalidCurrencyCodeException();
        }

        value = normalizedValue;

    }

    @Override
    public String toString() {
        return value;
    }
}
