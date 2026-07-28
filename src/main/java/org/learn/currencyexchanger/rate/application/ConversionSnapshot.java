package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.ConversionQuote;
import org.learn.currencyexchanger.rate.domain.Money;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ConversionSnapshot(
        Money source,
        Money target,
        BigDecimal referenceRate,
        LocalDate effectiveDate,
        Instant fetchedAt,
        boolean stale
) {
    public ConversionSnapshot {
        Objects.requireNonNull(
                source,
                "Source money cannot be null"
        );

        Objects.requireNonNull(
                target,
                "Target money cannot be null"
        );

        Objects.requireNonNull(
                referenceRate,
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
    }

    public static ConversionSnapshot from(
            ConversionQuote quote,
            boolean stale
    ) {
        Objects.requireNonNull(
                quote,
                "Conversion quote cannot be null"
        );

        ReferenceRate referenceRate =
                quote.referenceRate();

        return new ConversionSnapshot(
                quote.source(),
                quote.target(),
                referenceRate.value(),
                referenceRate.effectiveDate(),
                referenceRate.fetchedAt(),
                stale
        );
    }
}
