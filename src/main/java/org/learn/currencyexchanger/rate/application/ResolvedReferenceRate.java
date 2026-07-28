package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.util.Objects;

public record ResolvedReferenceRate(
        ReferenceRate referenceRate,
        boolean stale
) {

    public ResolvedReferenceRate {
        Objects.requireNonNull(
                referenceRate,
                "Reference rate cannot be null"
        );
    }

    public static ResolvedReferenceRate fresh(
            ReferenceRate referenceRate
    ) {
        return new ResolvedReferenceRate(
                referenceRate,
                false
        );
    }

    public static ResolvedReferenceRate stale(
            ReferenceRate referenceRate
    ) {
        return new ResolvedReferenceRate(
                referenceRate,
                true
        );
    }
}
