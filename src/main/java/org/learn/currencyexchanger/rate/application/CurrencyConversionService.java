package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.ConversionQuote;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class CurrencyConversionService {

    private final ReferenceRateResolver referenceRateResolver;

    public CurrencyConversionService(
            ReferenceRateResolver referenceRateResolver
    ) {
        this.referenceRateResolver = Objects.requireNonNull(
                referenceRateResolver,
                "Reference rate resolver cannot be null"
        );
    }

    public ConversionSnapshot convert(
            String base,
            String quote,
            BigDecimal amount
    ) {
        CurrencyPair pair = CurrencyPair.of(
                base,
                quote
        );

        Money source = new Money(
                pair.base(),
                amount
        );

        ResolvedReferenceRate resolvedRate =
                referenceRateResolver.resolve(pair);

        ConversionQuote conversionQuote =
                ConversionQuote.calculate(
                        source,
                        resolvedRate.referenceRate()
                );

        return ConversionSnapshot.from(
                conversionQuote,
                resolvedRate.stale()
        );
    }
}
