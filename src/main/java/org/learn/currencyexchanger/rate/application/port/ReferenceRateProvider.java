package org.learn.currencyexchanger.rate.application.port;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

public interface ReferenceRateProvider {
    ReferenceRate fetchLatest(CurrencyPair pair);
}
