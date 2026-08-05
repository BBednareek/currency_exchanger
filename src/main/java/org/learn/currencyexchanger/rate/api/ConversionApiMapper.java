package org.learn.currencyexchanger.rate.api;

import org.learn.currencyexchanger.rate.application.ConversionSnapshot;
import org.learn.currencyexchanger.rate.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ConversionApiMapper {

    private static final int TARGET_AMOUNT_MAX_SCALE = 8;
    private static final RoundingMode TARGET_AMOUNT_ROUNDING =
            RoundingMode.HALF_EVEN;

    private ConversionApiMapper() {
    }

    public static ConversionResponse toResponse(
            ConversionSnapshot snapshot
    ) {
        return new ConversionResponse(
                toMoneyResponse(snapshot.source()),
                toTargetMoneyResponse(snapshot.target()),
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

    private static MoneyResponse toTargetMoneyResponse(
            Money money
    ) {
        return new MoneyResponse(
                money.currency().value(),
                normalizeTargetAmount(
                        money.amount()
                )
        );
    }

    private static BigDecimal normalizeTargetAmount(
            BigDecimal amount
    ) {
        if (amount.scale() <= TARGET_AMOUNT_MAX_SCALE) {
            return amount;
        }

        return amount.setScale(
                TARGET_AMOUNT_MAX_SCALE,
                TARGET_AMOUNT_ROUNDING
        );
    }
}
