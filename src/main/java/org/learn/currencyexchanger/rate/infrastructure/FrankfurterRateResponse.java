package org.learn.currencyexchanger.rate.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;

record FrankfurterRateResponse(
        LocalDate date,
        String base,
        String quote,
        BigDecimal rate
) {
}
