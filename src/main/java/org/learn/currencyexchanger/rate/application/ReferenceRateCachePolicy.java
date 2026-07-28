package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class ReferenceRateCachePolicy {

    private final Duration timeToLive;
    private final Duration maximumFallbackAge;

    public ReferenceRateCachePolicy(
            Duration timeToLive,
            Duration maximumFallbackAge
    ) {
        this.timeToLive = requirePositive(
                timeToLive,
                "Cache time to live"
        );
        this.maximumFallbackAge = requirePositive(
                maximumFallbackAge,
                "Maximum fallback age"
        );

        if (maximumFallbackAge.compareTo(timeToLive) < 0) {
            throw new IllegalArgumentException(
                    "Maximum fallback age cannot be shorter" +
                            " than cache time to live"
            );
        }
    }

    private static Duration requirePositive(
            Duration duration,
            String name
    ) {
        Objects.requireNonNull(
                duration,
                name + " cannot be null"
        );

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must be greater than zero"
            );
        }

        return duration;
    }

    private static boolean isWithinAge(
            ReferenceRate referenceRate,
            Instant currentTime,
            Duration maximumAge
    ) {
        Objects.requireNonNull(
                referenceRate,
                "Reference rate cannot be null"
        );

        Objects.requireNonNull(
                currentTime,
                "Current time cannot be null"
        );

        Instant fetchedAt = referenceRate.fetchedAt();


        // Rekord z przyszlosci
        if (fetchedAt.isAfter(currentTime)) {
            return false;
        }

        Instant oldestAcceptedTimestamp =
                currentTime.minus(maximumAge);

        return !fetchedAt.isBefore(oldestAcceptedTimestamp);
    }

    public boolean isFresh(
            ReferenceRate referenceRate,
            Instant currentTime
    ) {
        return isWithinAge(
                referenceRate,
                currentTime,
                timeToLive
        );
    }

    public boolean isUsableAsFallback(
            ReferenceRate referenceRate,
            Instant currentTime
    ) {
        return isWithinAge(
                referenceRate,
                currentTime,
                maximumFallbackAge
        );
    }
}
