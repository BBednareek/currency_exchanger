package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.ConversionQuote;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CurrencyConversionService {
    private final ReferenceRateService referenceRateService;

    public CurrencyConversionService(
            ReferenceRateService referenceRateService
    ) {
        this.referenceRateService = referenceRateService;
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

        ReferenceRateSnapshot rateSnapshot =
                referenceRateService.getLatestRate(
                        pair.base().value(),
                        pair.quote().value()
                );

        ConversionQuote conversionQuote =
                ConversionQuote.calculate(
                        source,
                        rateSnapshot.asReferenceRate()
                );

        return ConversionSnapshot.from(
                conversionQuote,
                rateSnapshot.stale()
        );
    }
}
