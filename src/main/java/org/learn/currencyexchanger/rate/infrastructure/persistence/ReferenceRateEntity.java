package org.learn.currencyexchanger.rate.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "reference_rate")
class ReferenceRateEntity {
    @Id
    private UUID id;

    @Column(
            name = "base_currency",
            nullable = false,
            length = 3
    )
    private String baseCurrency;

    @Column(
            name = "quote_currency",
            nullable = false,
            length = 3
    )
    private String quoteCurrency;

    @Column(
            name = "rate",
            nullable = false,
            precision = ReferenceRate.MAX_PRECISION,
            scale = ReferenceRate.MAX_SCALE
    )
    private BigDecimal rate;

    @Column(
            name = "effective_date",
            nullable = false
    )
    private LocalDate effectiveDate;

    @Column(
            name = "fetched_at",
            nullable = false
    )
    private Instant fetchedAt;

    protected ReferenceRateEntity() {

    }

    ReferenceRate toDomain() {
        return new ReferenceRate(
                CurrencyPair.of(
                        baseCurrency,
                        quoteCurrency
                ),
                rate,
                effectiveDate,
                fetchedAt
        );
    }

}
