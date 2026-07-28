package org.learn.currencyexchanger.rate.infrastructure.persistence;

import org.learn.currencyexchanger.rate.application.port.ReferenceRateRepository;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaReferenceRateRepositoryAdapter
        implements ReferenceRateRepository {
    private final SpringDataReferenceRateRepository springDataReferenceRateRepository;

    JpaReferenceRateRepositoryAdapter(
            SpringDataReferenceRateRepository springDataReferenceRateRepository
    ) {
        this.springDataReferenceRateRepository = springDataReferenceRateRepository;
    }
    
    @Override
    public Optional<ReferenceRate> findLatest(CurrencyPair currencyPair) {
        Objects.requireNonNull(
                currencyPair,
                "Currency pair cannot be null"
        );

        return springDataReferenceRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByFetchedAtDescEffectiveDateDesc(
                currencyPair.base().value(),
                currencyPair.quote().value()
        ).map(
                ReferenceRateEntity::toDomain
        );
    }

    @Override
    @Transactional
    public ReferenceRate store(ReferenceRate referenceRate) {
        Objects.requireNonNull(
                referenceRate,
                "Reference rate cannot be null"
        );

        CurrencyPair currencyPair = referenceRate.pair();

        springDataReferenceRateRepository.upsert(
                UUID.randomUUID(),
                currencyPair.base().value(),
                currencyPair.quote().value(),
                referenceRate.value(),
                referenceRate.effectiveDate(),
                referenceRate.fetchedAt()
        );

        return springDataReferenceRateRepository.findByBaseCurrencyAndQuoteCurrencyAndEffectiveDate(
                currencyPair.base().value(),
                currencyPair.quote().value(),
                referenceRate.effectiveDate()
        ).map(
                ReferenceRateEntity::toDomain
        ).orElseThrow(
                () -> new IllegalStateException(
                        "Stored reference rate could not be reloaded"
                )
        );
    }
}
