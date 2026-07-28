package org.learn.currencyexchanger.rate.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.domain.exception.SourceCurrencyMismatchException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionQuoteTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final ReferenceRate REFERENCE_RATE =
            new ReferenceRate(
                    PAIR,
                    new BigDecimal("3.672100"),
                    LocalDate.of(2026, 7, 28),
                    Instant.parse(
                            "2026-07-28T12:00:00Z"
                    )
            );

    @Test
    void shouldCalculateConvertedAmount() {
        Money source = Money.of(
                "USD",
                new BigDecimal("100.00")
        );

        ConversionQuote quote =
                ConversionQuote.calculate(
                        source,
                        REFERENCE_RATE
                );

        Money target = quote.target();

        assertEquals(
                new CurrencyCode("PLN"),
                target.currency()
        );

        assertEquals(
                0,
                new BigDecimal("367.21000000")
                        .compareTo(target.amount())
        );
    }

    @Test
    void shouldPreserveSourceAndReferenceRate() {
        Money source = Money.of(
                "USD",
                new BigDecimal("125.50")
        );

        ConversionQuote quote =
                ConversionQuote.calculate(
                        source,
                        REFERENCE_RATE
                );

        assertSame(source, quote.source());

        assertSame(
                REFERENCE_RATE,
                quote.referenceRate()
        );
    }

    @Test
    void shouldRejectSourceCurrencyDifferentFromRateBase() {
        Money source = Money.of(
                "EUR",
                new BigDecimal("100.00")
        );

        SourceCurrencyMismatchException exception =
                assertThrows(
                        SourceCurrencyMismatchException.class,
                        () -> ConversionQuote.calculate(
                                source,
                                REFERENCE_RATE
                        )
                );

        assertEquals(
                "Source amount currency EUR does not match "
                        + "reference rate base currency USD",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullSource() {
        assertThrows(
                NullPointerException.class,
                () -> ConversionQuote.calculate(
                        null,
                        REFERENCE_RATE
                )
        );
    }

    @Test
    void shouldRejectNullReferenceRate() {
        Money source = Money.of(
                "USD",
                BigDecimal.ONE
        );

        assertThrows(
                NullPointerException.class,
                () -> ConversionQuote.calculate(
                        source,
                        null
                )
        );
    }
}
