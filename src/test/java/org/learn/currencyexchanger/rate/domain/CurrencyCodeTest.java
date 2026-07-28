package org.learn.currencyexchanger.rate.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyCodeTest {

    @Test
    void shouldCreateNormalizedCurrencyCode() {
        CurrencyCode currencyCode =
                new CurrencyCode("  pln  ");

        assertEquals("PLN", currencyCode.value());
    }

    @Test
    void shouldCompareCodesByNormalizedValue() {
        CurrencyCode first =
                new CurrencyCode("usd");

        CurrencyCode second =
                new CurrencyCode(" USD ");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldRenderAsNormalizedCode() {
        CurrencyCode currencyCode =
                new CurrencyCode("eur");

        assertEquals("EUR", currencyCode.toString());
    }

    @Test
    void shouldRejectNullCurrencyCode() {
        assertThrows(
                NullPointerException.class,
                () -> new CurrencyCode(null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "US",
            "USDD",
            "U1D",
            "U_D",
            "PŁN",
            "ＰＬＮ"
    })
    void shouldRejectInvalidCurrencyCode(String value) {
        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> new CurrencyCode(value)
        );
    }
}
