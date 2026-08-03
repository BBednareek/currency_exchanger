package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.InvalidReferenceRateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ReferenceRate(
        CurrencyPair pair,
        BigDecimal value,
        LocalDate effectiveDate,
        Instant fetchedAt
) {
    public static final int MAX_PRECISION = 38;
    public static final int MAX_SCALE = 18;

    private static final int MAX_INTEGER_DIGITS =
            MAX_PRECISION - MAX_SCALE;

    public ReferenceRate {
        Objects.requireNonNull(
                pair,
                "Currency pair cannot be null"
        );

        value = normalizeValue(value);

        Objects.requireNonNull(
                effectiveDate,
                "Effective date cannot be null"
        );

        Objects.requireNonNull(
                fetchedAt,
                "Fetch timestamp cannot be null"
        );
    }

    private static BigDecimal normalizeValue(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "Reference rate cannot be null"
        );

        BigDecimal normalizedValue =
                value.scale() > MAX_SCALE
                        ? value.setScale(
                        MAX_SCALE,
                        RoundingMode.HALF_EVEN
                )
                        : value;

        if (normalizedValue.signum() <= 0) {
            throw new InvalidReferenceRateException();
        }

        int integerDigits = Math.max(
                normalizedValue.precision()
                        - normalizedValue.scale(),
                0
        );

        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new InvalidReferenceRateException(
                    "Reference rate cannot exceed " +
                            MAX_INTEGER_DIGITS
                            + " integer digits"
            );
        }

        return normalizedValue;
    }
}
