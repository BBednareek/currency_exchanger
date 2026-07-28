package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceRateServiceTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final ReferenceRate REFERENCE_RATE =
            new ReferenceRate(
                    PAIR,
                    new BigDecimal("3.672100"),
                    LocalDate.of(2026, 7, 28),
                    Instant.parse("2026-07-28T12:00:00Z")
            );

    @Mock
    private ReferenceRateResolver referenceRateResolver;

    private ReferenceRateService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceRateService(
                referenceRateResolver
        );
    }

    @Test
    void shouldNormalizePairAndMapFreshRate() {
        when(referenceRateResolver.resolve(PAIR))
                .thenReturn(
                        ResolvedReferenceRate.fresh(
                                REFERENCE_RATE
                        )
                );

        ReferenceRateSnapshot result =
                service.getLatestRate(
                        " usd ",
                        "pln"
                );

        assertEquals(PAIR.base(), result.base());
        assertEquals(PAIR.quote(), result.quote());
        assertEquals(REFERENCE_RATE.value(), result.rate());
        assertEquals(
                REFERENCE_RATE.effectiveDate(),
                result.effectiveDate()
        );
        assertEquals(
                REFERENCE_RATE.fetchedAt(),
                result.fetchedAt()
        );
        assertFalse(result.stale());

        verify(referenceRateResolver).resolve(PAIR);
    }

    @Test
    void shouldMapStaleRate() {
        when(referenceRateResolver.resolve(PAIR))
                .thenReturn(
                        ResolvedReferenceRate.stale(
                                REFERENCE_RATE
                        )
                );

        ReferenceRateSnapshot result =
                service.getLatestRate(
                        "USD",
                        "PLN"
                );

        assertTrue(result.stale());

        verify(referenceRateResolver).resolve(PAIR);
    }

    @Test
    void shouldRejectInvalidCurrencyCodeBeforeResolvingRate() {
        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.getLatestRate(
                        "US",
                        "PLN"
                )
        );

        verifyNoInteractions(referenceRateResolver);
    }

    @Test
    void shouldRejectSameCurrencyPairBeforeResolvingRate() {
        assertThrows(
                InvalidCurrencyPairException.class,
                () -> service.getLatestRate(
                        "usd",
                        " USD "
                )
        );

        verifyNoInteractions(referenceRateResolver);
    }
}
