package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.Money;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;
import org.learn.currencyexchanger.rate.domain.exception.SourceCurrencyMismatchException;
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
class CurrencyConversionServiceTest {

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

    @Mock
    private ReferenceRateResolver referenceRateResolver;

    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyConversionService(
                referenceRateResolver
        );
    }

    @Test
    void shouldConvertAmountUsingReferenceRate() {
        when(referenceRateResolver.resolve(PAIR))
                .thenReturn(
                        ResolvedReferenceRate.fresh(
                                REFERENCE_RATE
                        )
                );

        ConversionSnapshot result = service.convert(
                " usd ",
                " pln ",
                new BigDecimal("100.00")
        );

        assertEquals(
                new Money(
                        PAIR.base(),
                        new BigDecimal("100.00")
                ),
                result.source()
        );

        assertEquals(
                PAIR.quote(),
                result.target().currency()
        );

        assertEquals(
                0,
                new BigDecimal("367.21000000")
                        .compareTo(
                                result.target().amount()
                        )
        );

        assertEquals(
                0,
                REFERENCE_RATE.value()
                        .compareTo(
                                result.referenceRate()
                        )
        );

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
    void shouldPropagateStaleInformation() {
        when(referenceRateResolver.resolve(PAIR))
                .thenReturn(
                        ResolvedReferenceRate.stale(
                                REFERENCE_RATE
                        )
                );

        ConversionSnapshot result = service.convert(
                "USD",
                "PLN",
                BigDecimal.TEN
        );

        assertTrue(result.stale());

        verify(referenceRateResolver).resolve(PAIR);
    }

    @Test
    void shouldRejectInvalidAmountBeforeResolvingRate() {
        assertThrows(
                InvalidMoneyAmountException.class,
                () -> service.convert(
                        "USD",
                        "PLN",
                        BigDecimal.ZERO
                )
        );

        verifyNoInteractions(referenceRateResolver);
    }

    @Test
    void shouldRejectInvalidCurrencyBeforeResolvingRate() {
        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.convert(
                        "US",
                        "PLN",
                        BigDecimal.TEN
                )
        );

        verifyNoInteractions(referenceRateResolver);
    }

    @Test
    void shouldRejectSameCurrencyPairBeforeResolvingRate() {
        assertThrows(
                InvalidCurrencyPairException.class,
                () -> service.convert(
                        "USD",
                        "usd",
                        BigDecimal.TEN
                )
        );

        verifyNoInteractions(referenceRateResolver);
    }

    @Test
    void shouldRejectInconsistentRateReturnedByDependency() {
        ReferenceRate inconsistentRate =
                new ReferenceRate(
                        CurrencyPair.of("EUR", "PLN"),
                        new BigDecimal("4.250000"),
                        LocalDate.of(2026, 7, 28),
                        Instant.parse(
                                "2026-07-28T12:00:00Z"
                        )
                );

        when(referenceRateResolver.resolve(PAIR))
                .thenReturn(
                        ResolvedReferenceRate.fresh(
                                inconsistentRate
                        )
                );

        assertThrows(
                SourceCurrencyMismatchException.class,
                () -> service.convert(
                        "USD",
                        "PLN",
                        BigDecimal.TEN
                )
        );

        verify(referenceRateResolver).resolve(PAIR);
    }
}
