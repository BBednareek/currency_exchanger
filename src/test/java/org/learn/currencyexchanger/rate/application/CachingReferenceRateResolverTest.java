package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.exception.UnsupportedCurrencyException;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateRepository;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingReferenceRateResolverTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-07-28T12:00:00Z");

    private static final ReferenceRate PROVIDER_RATE =
            rate(
                    "3.672100",
                    CURRENT_TIME
            );

    @Mock
    private ReferenceRateProvider referenceRateProvider;

    @Mock
    private ReferenceRateRepository referenceRateRepository;

    private CachingReferenceRateResolver resolver;

    private static ReferenceRate rate(
            String value,
            Instant fetchedAt
    ) {
        return new ReferenceRate(
                PAIR,
                new BigDecimal(value),
                LocalDate.of(2026, 7, 27),
                fetchedAt
        );
    }

    private static void assertResolvedRate(
            ReferenceRate expected,
            ResolvedReferenceRate actual,
            boolean stale
    ) {
        assertEquals(
                expected,
                actual.referenceRate()
        );

        if (stale) {
            assertTrue(actual.stale());
        } else {
            assertFalse(actual.stale());
        }
    }

    @Test
    void shouldReturnLatestStaleRateStoredByAnotherInstance() {
        ReferenceRate originallyCachedRate = rate(
                "3.620000",
                CURRENT_TIME.minus(
                        Duration.ofHours(3)
                )
        );

        ReferenceRate newerStaleRate = rate(
                "3.660000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(
                        Optional.of(originallyCachedRate),
                        Optional.of(originallyCachedRate),
                        Optional.of(newerStaleRate)
                );

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new RateProviderUnavailableException()
                );

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                newerStaleRate,
                result,
                true
        );

        verify(referenceRateRepository, never())
                .store(any());
    }

    @Test
    void shouldPropagateProviderFailureWhenCacheRemainsEmpty() {
        RateProviderUnavailableException expected =
                new RateProviderUnavailableException();

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.empty());

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(expected);

        RateProviderUnavailableException result =
                assertThrows(
                        RateProviderUnavailableException.class,
                        () -> resolver.resolve(PAIR)
                );

        assertSame(expected, result);

        verify(
                referenceRateRepository,
                times(3)
        ).findLatest(PAIR);

        verify(referenceRateRepository, never())
                .store(any());
    }

    @Test
    void shouldReturnFreshRateStoredByAnotherInstanceWhenProviderFails() {
        ReferenceRate expiredRate = rate(
                "3.640000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        ReferenceRate freshRateFromAnotherInstance = rate(
                "3.680000",
                CURRENT_TIME.minus(
                        Duration.ofMinutes(5)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(
                        Optional.of(expiredRate),
                        Optional.of(expiredRate),
                        Optional.of(freshRateFromAnotherInstance)
                );

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new RateProviderUnavailableException()
                );

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                freshRateFromAnotherInstance,
                result,
                false
        );

        verify(
                referenceRateRepository,
                times(3)
        ).findLatest(PAIR);

        verify(referenceRateRepository, never())
                .store(any());
    }

    @BeforeEach
    void setUp() {
        ReferenceRateCachePolicy cachePolicy =
                new ReferenceRateCachePolicy(
                        Duration.ofHours(1),
                        Duration.ofDays(7)
                );

        Clock clock = Clock.fixed(
                CURRENT_TIME,
                ZoneOffset.UTC
        );

        ReferenceRateRefreshCoordinator refreshCoordinator =
                new ReferenceRateRefreshCoordinator();


        resolver = new CachingReferenceRateResolver(
                referenceRateProvider,
                referenceRateRepository,
                cachePolicy,
                refreshCoordinator,
                clock
        );
    }

    @Test
    void shouldReturnFreshCachedRateWithoutCallingProvider() {
        ReferenceRate cachedRate = rate(
                "3.650000",
                CURRENT_TIME.minus(
                        Duration.ofMinutes(30)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(cachedRate));

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                cachedRate,
                result,
                false
        );

        verify(referenceRateRepository)
                .findLatest(PAIR);

        verify(referenceRateRepository, never())
                .store(any());

        verifyNoInteractions(referenceRateProvider);
    }

    @Test
    void shouldFetchAndStoreRateWhenCacheIsEmpty() {
        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.empty());

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenReturn(PROVIDER_RATE);

        when(referenceRateRepository.store(PROVIDER_RATE))
                .thenReturn(PROVIDER_RATE);

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                PROVIDER_RATE,
                result,
                false
        );

        //Pierwszy odczyt jest szybkim sprawdzeniem cache
        //Drugi zabezpiecza przed wyscigiem po wejsciu do sekcji single-flight
        verify(
                referenceRateRepository,
                times(2)
        ).findLatest(PAIR);

        verify(referenceRateProvider)
                .fetchLatest(PAIR);

        verify(referenceRateRepository)
                .store(PROVIDER_RATE);
    }

    @Test
    void shouldRefreshExpiredCachedRate() {
        ReferenceRate expiredRate = rate(
                "3.640000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(expiredRate));

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenReturn(PROVIDER_RATE);

        when(referenceRateRepository.store(PROVIDER_RATE))
                .thenReturn(PROVIDER_RATE);

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                PROVIDER_RATE,
                result,
                false
        );

        verify(referenceRateProvider)
                .fetchLatest(PAIR);

        verify(referenceRateRepository)
                .store(PROVIDER_RATE);
    }

    @Test
    void shouldReturnStaleFallbackWhenProviderIsUnavailable() {
        ReferenceRate fallbackRate = rate(
                "3.640000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(fallbackRate));

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new RateProviderUnavailableException()
                );

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                fallbackRate,
                result,
                true
        );

        verify(referenceRateRepository, never())
                .store(any());
    }

    @Test
    void shouldReturnStaleFallbackForInvalidProviderResponse() {
        ReferenceRate fallbackRate = rate(
                "3.640000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(fallbackRate));

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new InvalidRateProviderResponseException(
                                "Malformed provider payload"
                        )
                );

        ResolvedReferenceRate result =
                resolver.resolve(PAIR);

        assertResolvedRate(
                fallbackRate,
                result,
                true
        );

        verify(referenceRateRepository, never())
                .store(any());
    }

    @Test
    void shouldPropagateProviderFailureWhenFallbackIsTooOld() {
        ReferenceRate oldRate = rate(
                "3.600000",
                CURRENT_TIME.minus(
                        Duration.ofDays(8)
                )
        );

        RateProviderUnavailableException expected =
                new RateProviderUnavailableException();

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(oldRate));

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(expected);

        RateProviderUnavailableException result =
                assertThrows(
                        RateProviderUnavailableException.class,
                        () -> resolver.resolve(PAIR)
                );

        assertSame(expected, result);

        verify(referenceRateRepository, never())
                .store(any());
    }

    @Test
    void shouldNotUseFallbackForSemanticProviderError() {
        ReferenceRate fallbackRate = rate(
                "3.640000",
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        UnsupportedCurrencyException expected =
                new UnsupportedCurrencyException(PAIR);

        when(referenceRateRepository.findLatest(PAIR))
                .thenReturn(Optional.of(fallbackRate));

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(expected);

        UnsupportedCurrencyException result =
                assertThrows(
                        UnsupportedCurrencyException.class,
                        () -> resolver.resolve(PAIR)
                );

        assertSame(expected, result);

        verify(referenceRateRepository, never())
                .store(any());
    }

}
