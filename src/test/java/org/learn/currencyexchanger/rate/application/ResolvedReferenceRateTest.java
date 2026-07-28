package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedReferenceRateTest {

    private static final ReferenceRate REFERENCE_RATE =
            new ReferenceRate(
                    CurrencyPair.of("USD", "PLN"),
                    new BigDecimal("3.672100"),
                    LocalDate.of(2026, 7, 28),
                    Instant.parse("2026-07-28T12:00:00Z")
            );

    @Test
    void shouldCreateFreshResolvedRate() {
        ResolvedReferenceRate result =
                ResolvedReferenceRate.fresh(
                        REFERENCE_RATE
                );

        assertSame(
                REFERENCE_RATE,
                result.referenceRate()
        );

        assertFalse(result.stale());
    }

    @Test
    void shouldCreateStaleResolvedRate() {
        ResolvedReferenceRate result =
                ResolvedReferenceRate.stale(
                        REFERENCE_RATE
                );

        assertSame(
                REFERENCE_RATE,
                result.referenceRate()
        );

        assertTrue(result.stale());
    }

    @Test
    void shouldRejectMissingReferenceRate() {
        assertThrows(
                NullPointerException.class,
                () -> new ResolvedReferenceRate(
                        null,
                        false
                )
        );
    }
}
