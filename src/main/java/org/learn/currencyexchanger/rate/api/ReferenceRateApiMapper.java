package org.learn.currencyexchanger.rate.api;

import org.learn.currencyexchanger.rate.application.ReferenceRateSnapshot;

public class ReferenceRateApiMapper {

    private ReferenceRateApiMapper() {

    }

    public static ReferenceRateResponse toResponse(
            ReferenceRateSnapshot snapshot
    ) {
        return new ReferenceRateResponse(
                snapshot.base().value(),
                snapshot.quote().value(),
                snapshot.rate(),
                snapshot.effectiveDate(),
                snapshot.fetchedAt(),
                snapshot.stale()
        );
    }
}
