package org.learn.currencyexchanger.rate.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.learn.currencyexchanger.rate.domain.exception.InvalidReferenceRateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceRateTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final LocalDate EFFECTIVE_DATE =
            LocalDate.of(2026, 7, 27);

    private static final Instant FETCHED_AT =
            Instant.parse("2026-07-28T10:15:30Z");


    @Test
    void shouldRoundRateToMaximumScale() {
        ReferenceRate referenceRate =
                new ReferenceRate(
                        PAIR,
                        new BigDecimal(
                                "3.1234567890123456789"
                        ),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                );

        assertEquals(
                new BigDecimal(
                        "3.123456789012345679"
                ),
                referenceRate.value()
        );
    }

    @Test
    void shouldAcceptMaximumSupportedRate() {
        BigDecimal maximumSupportedValue =
                new BigDecimal(
                        "99999999999999999999."
                                + "999999999999999999"
                );

        ReferenceRate referenceRate =
                new ReferenceRate(
                        PAIR,
                        maximumSupportedValue,
                        EFFECTIVE_DATE,
                        FETCHED_AT
                );

        assertEquals(
                maximumSupportedValue,
                referenceRate.value()
        );
    }

    @Test
    void shouldRejectRateWithTooManyIntegerDigits() {
        assertThrows(
                InvalidReferenceRateException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal(
                                "100000000000000000000"
                        ),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }

    @Test
    void shouldRejectRateOverflowAfterRounding() {
        assertThrows(
                InvalidReferenceRateException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal(
                                "99999999999999999999."
                                        + "9999999999999999999"
                        ),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }


    @Test
    void shouldRejectRateRoundedToZero() {
        assertThrows(
                InvalidReferenceRateException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal(
                                "0.0000000000000000001"
                        ),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }
    
    @Test
    void shouldCreateReferenceRate() {
        BigDecimal value =
                new BigDecimal("3.672100");

        ReferenceRate referenceRate =
                new ReferenceRate(
                        PAIR,
                        value,
                        EFFECTIVE_DATE,
                        FETCHED_AT
                );

        assertAll(
                () -> assertSame(
                        PAIR,
                        referenceRate.pair()
                ),
                () -> assertEquals(
                        value,
                        referenceRate.value()
                ),
                () -> assertEquals(
                        EFFECTIVE_DATE,
                        referenceRate.effectiveDate()
                ),
                () -> assertEquals(
                        FETCHED_AT,
                        referenceRate.fetchedAt()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0",
            "0.0000",
            "-0.01",
            "-100"
    })
    void shouldRejectNonPositiveRate(String value) {
        assertThrows(
                InvalidReferenceRateException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal(value),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }

    @Test
    void shouldRejectNullPair() {
        assertThrows(
                NullPointerException.class,
                () -> new ReferenceRate(
                        null,
                        new BigDecimal("3.6721"),
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new ReferenceRate(
                        PAIR,
                        null,
                        EFFECTIVE_DATE,
                        FETCHED_AT
                )
        );
    }

    @Test
    void shouldRejectNullEffectiveDate() {
        assertThrows(
                NullPointerException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal("3.6721"),
                        null,
                        FETCHED_AT
                )
        );
    }

    @Test
    void shouldRejectNullFetchedAt() {
        assertThrows(
                NullPointerException.class,
                () -> new ReferenceRate(
                        PAIR,
                        new BigDecimal("3.6721"),
                        EFFECTIVE_DATE,
                        null
                )
        );
    }
}
