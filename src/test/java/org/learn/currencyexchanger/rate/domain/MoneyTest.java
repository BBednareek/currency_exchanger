package org.learn.currencyexchanger.rate.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithNormalizedCurrency() {
        Money money = Money.of(
                " usd ",
                new BigDecimal("100.00")
        );

        assertEquals(
                new CurrencyCode("USD"),
                money.currency()
        );

        assertEquals(
                new BigDecimal("100.00"),
                money.amount()
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                InvalidMoneyAmountException.class,
                () -> Money.of(
                        "USD",
                        BigDecimal.ZERO
                )
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                InvalidMoneyAmountException.class,
                () -> Money.of(
                        "USD",
                        new BigDecimal("-0.01")
                )
        );
    }

    @Test
    void shouldRejectNullCurrency() {
        assertThrows(
                NullPointerException.class,
                () -> new Money(
                        null,
                        BigDecimal.ONE
                )
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> new Money(
                        new CurrencyCode("USD"),
                        null
                )
        );
    }
}
