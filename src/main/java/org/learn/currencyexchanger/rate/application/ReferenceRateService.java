package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ReferenceRateService {

    private final ReferenceRateResolver referenceRateResolver;

    public ReferenceRateService(
            ReferenceRateResolver referenceRateResolver
    ) {
        this.referenceRateResolver = Objects.requireNonNull(
                referenceRateResolver,
                "Reference rate resolver cannot be null"
        );
    }

    public ReferenceRateSnapshot getLatestRate(
            String base,
            String quote
    ) {
        CurrencyPair pair = CurrencyPair.of(base, quote);

        ResolvedReferenceRate resolvedRate =
                referenceRateResolver.resolve(pair);

        return ReferenceRateSnapshot.from(
                resolvedRate
        );
    }
}
