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
        Instant fetchedAt
) {
    public static ReferenceRateSnapshot from(
            ReferenceRate referenceRate
    ) {
        Objects.requireNonNull(
                referenceRate,
                "Reference rate cannot be null"
        );

        return new ReferenceRateSnapshot(
                referenceRate.pair().base(),
                referenceRate.pair().quote(),
                referenceRate.value(),
                referenceRate.effectiveDate(),
                referenceRate.fetchedAt()
        );
    }
}
