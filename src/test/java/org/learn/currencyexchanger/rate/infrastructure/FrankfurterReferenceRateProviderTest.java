package org.learn.currencyexchanger.rate.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.exception.ReferenceRateNotFoundException;
import org.learn.currencyexchanger.rate.application.exception.UnsupportedCurrencyException;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FrankfurterReferenceRateProviderTest {

    private static final String BASE_URL =
            "https://api.frankfurter.dev/v2";

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final Instant FETCHED_AT =
            Instant.parse("2026-07-28T10:15:30Z");

    private static final int MAXIMUM_EFFECTIVE_DATE_AGE_DAYS = 7;

    private MockRestServiceServer server;
    private FrankfurterReferenceRateProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL);

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        Clock clock = Clock.fixed(
                FETCHED_AT,
                ZoneOffset.UTC
        );

        provider =
                new FrankfurterReferenceRateProvider(
                        builder.build(),
                        clock,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                );
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void shouldFetchAndMapReferenceRate() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                ))
                .andRespond(withSuccess(
                        """
                                {
                                  "date": "2026-07-27",
                                  "base": "USD",
                                  "quote": "PLN",
                                  "rate": 3.672100
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        ReferenceRate result =
                provider.fetchLatest(PAIR);

        assertAll(
                () -> assertEquals(
                        PAIR,
                        result.pair()
                ),
                () -> assertEquals(
                        new BigDecimal("3.672100"),
                        result.value()
                ),
                () -> assertEquals(
                        LocalDate.of(2026, 7, 27),
                        result.effectiveDate()
                ),
                () -> assertEquals(
                        FETCHED_AT,
                        result.fetchedAt()
                )
        );
    }

    @Test
    void shouldRejectFutureEffectiveDate() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withSuccess(
                        """
                                {
                                  "date": "2026-07-29",
                                  "base": "USD",
                                  "quote": "PLN",
                                  "rate": 3.672100
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectExcessivelyOldEffectiveDate() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withSuccess(
                        """
                                {
                                  "date": "2026-07-20",
                                  "base": "USD",
                                  "quote": "PLN",
                                  "rate": 3.672100
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectDifferentCurrencyPairInResponse() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withSuccess(
                        """
                                {
                                  "date": "2026-07-27",
                                  "base": "USD",
                                  "quote": "EUR",
                                  "rate": 0.8739
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectMissingResponseBody() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withNoContent());

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectNonPositiveRate() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withSuccess(
                        """
                                {
                                  "date": "2026-07-27",
                                  "base": "USD",
                                  "quote": "PLN",
                                  "rate": 0
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectMalformedJson() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withSuccess(
                        "{not-valid-json",
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                InvalidRateProviderResponseException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 422})
    void shouldTranslateUnsupportedCurrencyStatus(
            int status
    ) {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withStatus(
                        HttpStatusCode.valueOf(status)
                ));

        assertThrows(
                UnsupportedCurrencyException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldTranslateNotFoundStatus() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withStatus(
                        HttpStatusCode.valueOf(404)
                ));

        assertThrows(
                ReferenceRateNotFoundException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {
            401,
            403,
            429,
            500,
            502,
            503,
            504
    })
    void shouldTranslateProviderFailureStatus(
            int status
    ) {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withStatus(
                        HttpStatusCode.valueOf(status)
                ));

        assertThrows(
                RateProviderUnavailableException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldTranslateConnectionFailure() {
        server.expect(
                        requestTo(
                                BASE_URL + "/rate/USD/PLN"
                        )
                )
                .andRespond(withException(
                        new IOException(
                                "Simulated connection failure"
                        )
                ));

        assertThrows(
                RateProviderUnavailableException.class,
                () -> provider.fetchLatest(PAIR)
        );
    }

    @Test
    void shouldRejectNullCurrencyPair() {
        assertThrows(
                NullPointerException.class,
                () -> provider.fetchLatest(null)
        );
    }
}
