package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;

public interface ReferenceRateResolver {
    ResolvedReferenceRate resolve(CurrencyPair pair);
}
