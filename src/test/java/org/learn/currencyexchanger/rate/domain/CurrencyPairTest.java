package org.learn.currencyexchanger.rate.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyPairTest {

    @Test
    void shouldCreateCurrencyPair() {
        CurrencyCode base = new CurrencyCode("USD");
        CurrencyCode quote = new CurrencyCode("PLN");

        CurrencyPair pair =
                new CurrencyPair(base, quote);

        assertSame(base, pair.base());
        assertSame(quote, pair.quote());
        assertEquals("USD/PLN", pair.symbol());
        assertEquals("USD/PLN", pair.toString());
    }

    @Test
    void shouldCreatePairFromRawCodes() {
        CurrencyPair pair =
                CurrencyPair.of(" usd ", "pln");

        assertEquals(
                new CurrencyCode("USD"),
                pair.base()
        );

        assertEquals(
                new CurrencyCode("PLN"),
                pair.quote()
        );
    }

    @Test
    void shouldRejectNullBaseCurrency() {
        assertThrows(
                NullPointerException.class,
                () -> new CurrencyPair(
                        null,
                        new CurrencyCode("PLN")
                )
        );
    }

    @Test
    void shouldRejectNullQuoteCurrency() {
        assertThrows(
                NullPointerException.class,
                () -> new CurrencyPair(
                        new CurrencyCode("USD"),
                        null
                )
        );
    }

    @Test
    void shouldRejectPairWithSameCurrencies() {
        assertThrows(
                InvalidCurrencyPairException.class,
                () -> CurrencyPair.of(
                        "USD",
                        "usd"
                )
        );
    }
}
