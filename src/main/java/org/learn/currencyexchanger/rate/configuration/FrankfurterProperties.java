package org.learn.currencyexchanger.rate.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "frankfurter")
public record FrankfurterProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public FrankfurterProperties {
        validateBaseURL(baseUrl);

        requirePositive(
                connectTimeout,
                "Connect timeout"
        );

        requirePositive(
                readTimeout,
                "Read timeout"
        );
    }

    private static void validateBaseURL(URI baseURL) {
        Objects.requireNonNull(
                baseURL,
                "Frankfurter base URL cannot be null"
        );

        boolean validHttpsURL =
                baseURL
                        .isAbsolute()
                        && baseURL.getHost() != null
                        && "https".equalsIgnoreCase(
                        baseURL.getScheme()
                );

        if (!validHttpsURL) {
            throw new IllegalArgumentException(
                    "Frankfurter base URL must be an absolute HTTPS URL"
            );
        }

        if (baseURL.getQuery() != null
                || baseURL.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Frankfurter base URL cannot contain a query or fragment"
            );
        }


    }

    private static void requirePositive(
            Duration duration,
            String propertyName) {
        Objects.requireNonNull(
                duration,
                "Duration cannot be null"
        );

        Objects.requireNonNull(
                propertyName,
                "Property name cannot be null"
        );

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    propertyName + " must be greater than zero"
            );
        }
    }
}
