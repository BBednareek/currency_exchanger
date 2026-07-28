package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceRateCachePolicyTest {

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-07-28T12:00:00Z");

    private final ReferenceRateCachePolicy policy =
            new ReferenceRateCachePolicy(
                    Duration.ofHours(1),
                    Duration.ofDays(7)
            );

    private static ReferenceRate rateFetchedAt(
            Instant fetchedAt
    ) {
        return new ReferenceRate(
                CurrencyPair.of("USD", "PLN"),
                new BigDecimal("3.672100"),
                LocalDate.of(2026, 7, 27),
                fetchedAt
        );
    }

    @Test
    void shouldTreatRecentlyFetchedRateAsFresh() {
        ReferenceRate rate = rateFetchedAt(
                CURRENT_TIME.minus(
                        Duration.ofMinutes(30)
                )
        );

        assertTrue(
                policy.isFresh(rate, CURRENT_TIME)
        );
    }

    @Test
    void shouldTreatRateAtTtlBoundaryAsFresh() {
        ReferenceRate rate = rateFetchedAt(
                CURRENT_TIME.minus(
                        Duration.ofHours(1)
                )
        );

        assertTrue(
                policy.isFresh(rate, CURRENT_TIME)
        );
    }

    @Test
    void shouldTreatExpiredRateAsFallbackCandidate() {
        ReferenceRate rate = rateFetchedAt(
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        assertFalse(
                policy.isFresh(rate, CURRENT_TIME)
        );

        assertTrue(
                policy.isUsableAsFallback(
                        rate,
                        CURRENT_TIME
                )
        );
    }

    @Test
    void shouldRejectRateOlderThanMaximumFallbackAge() {
        ReferenceRate rate = rateFetchedAt(
                CURRENT_TIME.minus(
                        Duration.ofDays(8)
                )
        );

        assertFalse(
                policy.isFresh(rate, CURRENT_TIME)
        );

        assertFalse(
                policy.isUsableAsFallback(
                        rate,
                        CURRENT_TIME
                )
        );
    }

    @Test
    void shouldRejectRateFetchedInTheFuture() {
        ReferenceRate rate = rateFetchedAt(
                CURRENT_TIME.plusSeconds(1)
        );

        assertFalse(
                policy.isFresh(rate, CURRENT_TIME)
        );

        assertFalse(
                policy.isUsableAsFallback(
                        rate,
                        CURRENT_TIME
                )
        );
    }

    @Test
    void shouldRejectNonPositiveTimeToLive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReferenceRateCachePolicy(
                        Duration.ZERO,
                        Duration.ofDays(7)
                )
        );
    }

    @Test
    void shouldRejectFallbackAgeShorterThanTimeToLive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReferenceRateCachePolicy(
                        Duration.ofHours(2),
                        Duration.ofHours(1)
                )
        );
    }
}
