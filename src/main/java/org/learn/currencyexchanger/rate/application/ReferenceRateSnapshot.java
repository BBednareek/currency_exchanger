package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.CurrencyCode;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ReferenceRateSnapshot(
        CurrencyCode base,
        CurrencyCode quote,
        BigDecimal rate,
        LocalDate effectiveDate,
        Instant fetchedAt,
        boolean stale
) {

    public ReferenceRateSnapshot {
        Objects.requireNonNull(
                base,
                "Base currency cannot be null"
        );

        Objects.requireNonNull(
                quote,
                "Quote currency cannot be null"
        );

        Objects.requireNonNull(
                rate,
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

    public static ReferenceRateSnapshot from(
            ResolvedReferenceRate resolvedRate
    ) {
        Objects.requireNonNull(
                resolvedRate,
                "Resolved reference rate cannot be null"
        );

        ReferenceRate referenceRate =
                resolvedRate.referenceRate();

        return new ReferenceRateSnapshot(
                referenceRate.pair().base(),
                referenceRate.pair().quote(),
                referenceRate.value(),
                referenceRate.effectiveDate(),
                referenceRate.fetchedAt(),
                resolvedRate.stale()
        );
    }
}
