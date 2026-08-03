package org.learn.currencyexchanger.rate.infrastructure.persistence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

interface SpringDataReferenceRateRepository
        extends Repository<ReferenceRateEntity, UUID> {

    Optional<ReferenceRateEntity> findFirstByBaseCurrencyAndQuoteCurrencyOrderByFetchedAtDescEffectiveDateDesc(
            String baseCurrency,
            String quoteCurrency
    );

    Optional<ReferenceRateEntity> findByBaseCurrencyAndQuoteCurrencyAndEffectiveDate(
            String baseCurrency,
            String quoteCurrency,
            LocalDate effectiveDate
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(
            value = """
                    INSERT INTO reference_rate AS stored_rate (
                        id,
                        base_currency,
                        quote_currency,
                        rate,
                        effective_date,
                        fetched_at
                    )
                    VALUES (
                        :id,
                        :baseCurrency,
                        :quoteCurrency,
                        :rate,
                        :effectiveDate,
                        :fetchedAt
                    )
                    ON CONFLICT (
                        base_currency,
                        quote_currency,
                        effective_date
                    )
                    DO UPDATE SET
                        rate = EXCLUDED.rate,
                        fetched_at = EXCLUDED.fetched_at
                    WHERE stored_rate.fetched_at
                            < EXCLUDED.fetched_at
                    """,
            nativeQuery = true
    )
    int upsert(
            @Param("id")
            UUID id,

            @Param("baseCurrency")
            String baseCurrency,

            @Param("quoteCurrency")
            String quoteCurrency,

            @Param("rate")
            BigDecimal rate,

            @Param("effectiveDate")
            LocalDate effectiveDate,

            @Param("fetchedAt")
            Instant fetchedAt
    );
}
