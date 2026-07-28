package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateRepository;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ReferenceRateService {


    private static final Logger log = LoggerFactory.getLogger(ReferenceRateService.class);

    private final ReferenceRateProvider referenceRateProvider;
    private final ReferenceRateRepository referenceRateRepository;
    private final ReferenceRateCachePolicy referenceRateCachePolicy;
    private final ReferenceRateRefreshCoordinator referenceRateRefreshCoordinator;
    private final Clock clock;

    public ReferenceRateService(
            ReferenceRateProvider referenceRateProvider,
            ReferenceRateRepository referenceRateRepository,
            ReferenceRateCachePolicy referenceRateCachePolicy,
            ReferenceRateRefreshCoordinator referenceRateRefreshCoordinator,
            Clock clock
    ) {
        this.referenceRateProvider = referenceRateProvider;
        this.referenceRateRepository = referenceRateRepository;
        this.referenceRateCachePolicy = referenceRateCachePolicy;
        this.referenceRateRefreshCoordinator = referenceRateRefreshCoordinator;
        this.clock = clock;
    }

    public ReferenceRateSnapshot getLatestRate(
            String base,
            String quote
    ) {
        CurrencyPair pair = CurrencyPair.of(base, quote);
        Instant currentTime = clock.instant();

        Optional<ReferenceRateSnapshot> cachedResult =
                findFreshSnapshot(
                        pair,
                        currentTime
                );

        if (cachedResult.isPresent()) {
            return cachedResult.orElseThrow();
        }

        return referenceRateRefreshCoordinator.execute(
                pair,
                () -> refreshAfterCacheMiss(pair)
        );
    }

    private ReferenceRateSnapshot refreshAfterCacheMiss(
            CurrencyPair pair
    ) {
        Instant currentTime = clock.instant();

        Optional<ReferenceRate> cachedRate =
                referenceRateRepository.findLatest(pair);

        Optional<ReferenceRate> freshRate =
                cachedRate.filter(
                        rate -> referenceRateCachePolicy.isFresh(
                                rate,
                                currentTime
                        )
                );

        //Inne zadanie moglo odswiezyc baze pomiedzy pierwszym sprawdzeniem
        if (freshRate.isPresent()) {
            return ReferenceRateSnapshot.fresh(
                    freshRate.orElseThrow()
            );
        }

        try {
            ReferenceRate fetchedRate =
                    referenceRateProvider.fetchLatest(pair);

            ReferenceRate storedRate =
                    referenceRateRepository.store(
                            fetchedRate
                    );

            return ReferenceRateSnapshot.fresh(
                    storedRate
            );
        } catch (
                RateProviderUnavailableException
                | InvalidRateProviderResponseException exception
        ) {
            return useFallbackOrRethrow(
                    pair,
                    cachedRate,
                    currentTime,
                    exception
            );
        }
    }

    private Optional<ReferenceRateSnapshot> findFreshSnapshot(
            CurrencyPair pair,
            Instant currentTime
    ) {
        return referenceRateRepository
                .findLatest(pair)
                .filter(
                        rate -> referenceRateCachePolicy.isFresh(
                                rate,
                                currentTime
                        )
                )
                .map(ReferenceRateSnapshot::fresh);
    }

    private ReferenceRateSnapshot useFallbackOrRethrow(
            CurrencyPair currencyPair,
            Optional<ReferenceRate> cachedRate,
            Instant currentTime,
            RuntimeException providerFailure
    ) {
        ReferenceRate fallbackRate =
                cachedRate.filter(
                        rate -> referenceRateCachePolicy.isUsableAsFallback(
                                rate,
                                currentTime
                        )
                ).orElseThrow(() -> providerFailure);

        log.warn(
                "Using stale reference rate for {} fetched at {}",
                currencyPair.symbol(),
                fallbackRate.fetchedAt()
        );

        log.debug(
                "Reference rate provider failure details",
                providerFailure
        );

        return ReferenceRateSnapshot.stale(
                fallbackRate
        );
    }
}
