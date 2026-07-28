package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.springframework.stereotype.Service;

@Service
public class ReferenceRateService {

    private final ReferenceRateProvider referenceRateProvider;

    public ReferenceRateService(
            ReferenceRateProvider referenceRateProvider
    ) {
        this.referenceRateProvider = referenceRateProvider;
    }

    public ReferenceRateSnapshot getLatestRate(
            String base,
            String quote
    ) {
        CurrencyPair pair = CurrencyPair.of(base, quote);

        ReferenceRate referenceRate =
                referenceRateProvider.fetchLatest(pair);

        return ReferenceRateSnapshot.from(referenceRate);
    }
}
