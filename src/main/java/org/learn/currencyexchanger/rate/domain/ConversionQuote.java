package org.learn.currencyexchanger.rate.domain;

import org.learn.currencyexchanger.rate.domain.exception.SourceCurrencyMismatchException;

import java.math.MathContext;
import java.util.Objects;

public record ConversionQuote(
        Money source,
        ReferenceRate referenceRate
) {
    private static final MathContext CALCULATION_CONTEXT =
            MathContext.DECIMAL128;

    public ConversionQuote {
        Objects.requireNonNull(
                source,
                "Source money cannot be null"
        );

        Objects.requireNonNull(
                referenceRate,
                "Reference rate cannot be null"
        );

        CurrencyCode expectedSourceCurrency =
                referenceRate.pair().base();

        if (!source.currency().equals(
                expectedSourceCurrency
        )) {
            throw new SourceCurrencyMismatchException(
                    source.currency(),
                    expectedSourceCurrency
            );
        }
    }

    public static ConversionQuote calculate(
            Money source,
            ReferenceRate referenceRate
    ) {
        return new ConversionQuote(
                source,
                referenceRate
        );
    }

    public Money target() {
        return new Money(
                referenceRate.pair().quote(),
                source.amount().multiply(
                        referenceRate.value(),
                        CALCULATION_CONTEXT
                )
        );
    }
}
