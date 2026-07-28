package org.learn.currencyexchanger.rate.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "reference-rate.cache")
public record ReferenceRateCacheProperties(
        Duration timeToLive,
        Duration maximumFallbackAge
) {
    public ReferenceRateCacheProperties {
        requirePositive(
                timeToLive,
                "Reference rate cache time to live"
        );

        requirePositive(
                maximumFallbackAge,
                "Reference rate maximum fallback age"
        );

        if (maximumFallbackAge.compareTo(timeToLive) < 0) {
            throw new IllegalArgumentException(
                    "Maximum fallback age cannot be shorter" +
                            " than cache time to live"
            );
        }
    }

    private static void requirePositive(
            Duration duration,
            String propertyName
    ) {
        Objects.requireNonNull(
                duration,
                propertyName + " cannot be null"
        );

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    propertyName + " must be greater than zero"
            );
        }
    }
}
