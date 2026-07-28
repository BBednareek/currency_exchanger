package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.InvalidReferenceRateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ReferenceRate(
        CurrencyPair pair,
        BigDecimal value,
        LocalDate effectiveDate,
        Instant fetchedAt
) {

    public ReferenceRate {
        Objects.requireNonNull(
                pair,
                "Currency pair cannot be null"
        );

        Objects.requireNonNull(
                value,
                "Reference rate cannot be null"
        );

        Objects.requireNonNull(
                effectiveDate,
                "Effective date cannot be null"
        );

        Objects.requireNonNull(
                fetchedAt,
                "Fetch timestamp cannot be null"
        );

        if (value.signum() <= 0) {
            throw new InvalidReferenceRateException();
        }
    }
}
