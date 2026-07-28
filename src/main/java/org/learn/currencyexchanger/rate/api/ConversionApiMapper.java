package org.learn.currencyexchanger.rate.api;

import org.learn.currencyexchanger.rate.application.ConversionSnapshot;
import org.learn.currencyexchanger.rate.domain.Money;

public final class ConversionApiMapper {

    private ConversionApiMapper() {
    }

    public static ConversionResponse toResponse(
            ConversionSnapshot snapshot
    ) {
        return new ConversionResponse(
                toMoneyResponse(snapshot.source()),
                toMoneyResponse(snapshot.target()),
                snapshot.referenceRate(),
                snapshot.effectiveDate(),
                snapshot.fetchedAt(),
                snapshot.stale()
        );
    }

    private static MoneyResponse toMoneyResponse(
            Money money
    ) {
        return new MoneyResponse(
                money.currency().value(),
                money.amount()
        );
    }
}
