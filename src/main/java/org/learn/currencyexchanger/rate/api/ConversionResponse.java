package org.learn.currencyexchanger.rate.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ConversionResponse(
        MoneyResponse source,
        MoneyResponse target,
        BigDecimal referenceRate,
        LocalDate effectiveDate,
        Instant fetchedAt,
        boolean stale
) {
}
