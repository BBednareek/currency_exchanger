package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceRateServiceTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final ReferenceRate RATE =
            new ReferenceRate(
                    PAIR,
                    new BigDecimal("3.672100"),
                    LocalDate.of(2026, 7, 27),
                    Instant.parse("2026-07-28T10:15:30Z")
            );

    @Mock
    private ReferenceRateProvider referenceRateProvider;

    private ReferenceRateService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceRateService(
                referenceRateProvider
        );
    }

    @Test
    void shouldFetchLatestRateForNormalizedCurrencyPair() {
        when(referenceRateProvider.fetchLatest(PAIR))
                .thenReturn(RATE);

        ReferenceRateSnapshot result =
                service.getLatestRate(" usd ", "pln");

        assertAll(
                () -> assertEquals(PAIR.base(), result.base()),
                () -> assertEquals(PAIR.quote(), result.quote()),
                () -> assertEquals(
                        new BigDecimal("3.672100"),
                        result.rate()
                ),
                () -> assertEquals(
                        LocalDate.of(2026, 7, 27),
                        result.effectiveDate()
                ),
                () -> assertEquals(
                        Instant.parse("2026-07-28T10:15:30Z"),
                        result.fetchedAt()
                )
        );

        verify(referenceRateProvider).fetchLatest(PAIR);
    }

    @Test
    void shouldRejectInvalidCurrencyCodeBeforeCallingProvider() {
        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.getLatestRate("US", "PLN")
        );

        verifyNoInteractions(referenceRateProvider);
    }

    @Test
    void shouldRejectPairContainingTheSameCurrency() {
        assertThrows(
                InvalidCurrencyPairException.class,
                () -> service.getLatestRate("usd", " USD ")
        );

        verifyNoInteractions(referenceRateProvider);
    }

    @Test
    void shouldPropagateProviderFailure() {
        RateProviderUnavailableException expected =
                new RateProviderUnavailableException();

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(expected);

        RateProviderUnavailableException result =
                assertThrows(
                        RateProviderUnavailableException.class,
                        () -> service.getLatestRate("USD", "PLN")
                );

        assertSame(expected, result);

        verify(referenceRateProvider).fetchLatest(PAIR);
    }
}
