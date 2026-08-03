package org.learn.currencyexchanger.rate.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrankfurterPropertiesTest {

    private static final URI BASE_URL =
            URI.create(
                    "https://api.frankfurter.dev/v2"
            );

    private static final int MAXIMUM_EFFECTIVE_DATE_AGE_DAYS = 7;

    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(2);

    private static final Duration READ_TIMEOUT =
            Duration.ofSeconds(3);

    private static Stream<URI> invalidBaseUrls() {
        return Stream.of(
                URI.create(
                        "http://api.frankfurter.dev/v2"
                ),
                URI.create("/v2"),
                URI.create("https:///v2")
        );
    }

    private static Stream<Duration> invalidTimeouts() {
        return Stream.of(
                Duration.ZERO,
                Duration.ofSeconds(-1)
        );
    }

    @Test
    void shouldCreateValidProperties() {
        FrankfurterProperties properties =
                new FrankfurterProperties(
                        BASE_URL,
                        CONNECT_TIMEOUT,
                        READ_TIMEOUT,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                );

        assertAll(
                () -> assertEquals(
                        BASE_URL,
                        properties.baseUrl()
                ),
                () -> assertEquals(
                        CONNECT_TIMEOUT,
                        properties.connectTimeout()
                ),
                () -> assertEquals(
                        READ_TIMEOUT,
                        properties.readTimeout()
                ),
                () -> assertEquals(
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS,
                        properties.maximumEffectiveDateAgeDays()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBaseUrls")
    void shouldRejectInvalidBaseUrl(URI baseUrl) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        baseUrl,
                        CONNECT_TIMEOUT,
                        READ_TIMEOUT,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                )
        );
    }

    @Test
    void shouldRejectBaseUrlWithQuery() {
        URI baseUrl = URI.create(
                "https://api.frankfurter.dev/v2?test=true"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        baseUrl,
                        CONNECT_TIMEOUT,
                        READ_TIMEOUT,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                )
        );
    }

    @Test
    void shouldRejectBaseUrlWithFragment() {
        URI baseUrl = URI.create(
                "https://api.frankfurter.dev/v2#rates"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        baseUrl,
                        CONNECT_TIMEOUT,
                        READ_TIMEOUT,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTimeouts")
    void shouldRejectInvalidConnectTimeout(
            Duration timeout
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        BASE_URL,
                        timeout,
                        READ_TIMEOUT,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTimeouts")
    void shouldRejectInvalidReadTimeout(
            Duration timeout
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        BASE_URL,
                        CONNECT_TIMEOUT,
                        timeout,
                        MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                )
        );
    }

    @Test
    void shouldRejectNullValues() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new FrankfurterProperties(
                                null,
                                CONNECT_TIMEOUT,
                                READ_TIMEOUT,
                                MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new FrankfurterProperties(
                                BASE_URL,
                                null,
                                READ_TIMEOUT,
                                MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new FrankfurterProperties(
                                BASE_URL,
                                CONNECT_TIMEOUT,
                                null,
                                MAXIMUM_EFFECTIVE_DATE_AGE_DAYS
                        )
                )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectInvalidMaximumEffectiveDateAge(
            int maximumAgeDays
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FrankfurterProperties(
                        BASE_URL,
                        CONNECT_TIMEOUT,
                        READ_TIMEOUT,
                        maximumAgeDays
                )
        );
    }
}
