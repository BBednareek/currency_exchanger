package org.learn.currencyexchanger.rate.application.port;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.util.Optional;

public interface ReferenceRateRepository {
    Optional<ReferenceRate> findLatest(
            CurrencyPair currencyPair
    );

    ReferenceRate store(
            ReferenceRate referenceRate
    );
}
