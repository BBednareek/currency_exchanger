package org.learn.currencyexchanger.rate.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReferenceRateResponse(
        String base,
        String quote,
        BigDecimal rate,
        LocalDate effectiveDate,
        Instant fetchedAt
) {
}
