package org.learn.currencyexchanger.rate.api;

import java.math.BigDecimal;

public record MoneyResponse(
        String currency,
        BigDecimal amount
) {
}
