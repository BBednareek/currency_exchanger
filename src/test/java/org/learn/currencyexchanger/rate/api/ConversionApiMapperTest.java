package org.learn.currencyexchanger.rate.api;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.application.ConversionSnapshot;
import org.learn.currencyexchanger.rate.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionApiMapperTest {

    private static final LocalDate EFFECTIVE_DATE =
            LocalDate.of(2026, 7, 28);

    private static final Instant FETCHED_AT =
            Instant.parse(
                    "2026-07-28T12:00:00Z"
            );

    @Test
    void shouldRoundTargetAmountToEightFractionalDigits() {
        ConversionSnapshot snapshot =
                new ConversionSnapshot(
                        Money.of(
                                "USD",
                                new BigDecimal("100.00")
                        ),
                        Money.of(
                                "PLN",
                                new BigDecimal(
                                        "367.123456789012345678"
                                )
                        ),
                        new BigDecimal(
                                "3.671234567890123456"
                        ),
                        EFFECTIVE_DATE,
                        FETCHED_AT,
                        false
                );

        ConversionResponse response =
                ConversionApiMapper.toResponse(snapshot);

        assertAll(
                () -> assertEquals(
                        "USD",
                        response.source().currency()
                ),
                () -> assertEquals(
                        new BigDecimal("100.00"),
                        response.source().amount()
                ),
                () -> assertEquals(
                        "PLN",
                        response.target().currency()
                ),
                () -> assertEquals(
                        new BigDecimal("367.12345679"),
                        response.target().amount()
                ),
                () -> assertEquals(
                        new BigDecimal(
                                "3.671234567890123456"
                        ),
                        response.referenceRate()
                ),
                () -> assertEquals(
                        EFFECTIVE_DATE,
                        response.effectiveDate()
                ),
                () -> assertEquals(
                        FETCHED_AT,
                        response.fetchedAt()
                ),
                () -> assertFalse(response.stale())
        );
    }

    @Test
    void shouldUseHalfEvenRoundingForTargetAmount() {
        ConversionSnapshot snapshot =
                new ConversionSnapshot(
                        Money.of(
                                "USD",
                                BigDecimal.ONE
                        ),
                        Money.of(
                                "PLN",
                                new BigDecimal(
                                        "1.123456785"
                                )
                        ),
                        BigDecimal.ONE,
                        EFFECTIVE_DATE,
                        FETCHED_AT,
                        false
                );

        ConversionResponse response =
                ConversionApiMapper.toResponse(snapshot);

        assertEquals(
                new BigDecimal("1.12345678"),
                response.target().amount()
        );
    }

    @Test
    void shouldPreserveTargetAmountWithinScaleLimit() {
        BigDecimal targetAmount =
                new BigDecimal("367.21000000");

        ConversionSnapshot snapshot =
                new ConversionSnapshot(
                        Money.of(
                                "USD",
                                new BigDecimal("100.00")
                        ),
                        Money.of(
                                "PLN",
                                targetAmount
                        ),
                        new BigDecimal("3.672100"),
                        EFFECTIVE_DATE,
                        FETCHED_AT,
                        true
                );

        ConversionResponse response =
                ConversionApiMapper.toResponse(snapshot);

        assertAll(
                () -> assertEquals(
                        targetAmount,
                        response.target().amount()
                ),
                () -> assertEquals(
                        8,
                        response.target().amount().scale()
                ),
                () -> assertTrue(response.stale())
        );
    }
}
