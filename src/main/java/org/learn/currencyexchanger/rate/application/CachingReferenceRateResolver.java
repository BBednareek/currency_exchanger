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
import java.util.Objects;
import java.util.Optional;

@Service
public class CachingReferenceRateResolver
        implements ReferenceRateResolver {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CachingReferenceRateResolver.class
            );

    private final ReferenceRateProvider referenceRateProvider;
    private final ReferenceRateRepository referenceRateRepository;
    private final ReferenceRateCachePolicy referenceRateCachePolicy;
    private final ReferenceRateRefreshCoordinator referenceRateRefreshCoordinator;
    private final Clock clock;

    public CachingReferenceRateResolver(
            ReferenceRateProvider referenceRateProvider,
            ReferenceRateRepository referenceRateRepository,
            ReferenceRateCachePolicy referenceRateCachePolicy,
            ReferenceRateRefreshCoordinator referenceRateRefreshCoordinator,
            Clock clock
    ) {
        this.referenceRateProvider = Objects.requireNonNull(
                referenceRateProvider,
                "Reference rate provider cannot be null"
        );
        this.referenceRateRepository = Objects.requireNonNull(
                referenceRateRepository,
                "Reference rate repository cannot be null"
        );
        this.referenceRateCachePolicy = Objects.requireNonNull(
                referenceRateCachePolicy,
                "Reference rate cache policy cannot be null"
        );
        this.referenceRateRefreshCoordinator = Objects.requireNonNull(
                referenceRateRefreshCoordinator,
                "Reference rate refresh coordinator cannot be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null"
        );
    }

    @Override
    public ResolvedReferenceRate resolve(
            CurrencyPair pair
    ) {
        Objects.requireNonNull(
                pair,
                "Currency pair cannot be null"
        );

        Instant currentTime = clock.instant();

        Optional<ResolvedReferenceRate> cachedResult =
                findFreshRate(
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

    private Optional<ResolvedReferenceRate> findFreshRate(
            CurrencyPair pair,
            Instant currentTime
    ) {
        return referenceRateRepository
                .findLatest(pair)
                .filter(
                        rate -> referenceRateCachePolicy
                                .isFresh(
                                        rate,
                                        currentTime
                                )
                )
                .map(ResolvedReferenceRate::fresh);
    }

    private ResolvedReferenceRate refreshAfterCacheMiss(
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

        if (freshRate.isPresent()) {
            return ResolvedReferenceRate.fresh(
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

            return ResolvedReferenceRate.fresh(
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

    private ResolvedReferenceRate useFallbackOrRethrow(
            CurrencyPair pair,
            Optional<ReferenceRate> cachedRate,
            Instant currentTime,
            RuntimeException providerFailure
    ) {
        ReferenceRate fallbackRate =
                cachedRate.filter(
                                rate -> referenceRateCachePolicy
                                        .isUsableAsFallback(
                                                rate,
                                                currentTime
                                        )
                        )
                        .orElseThrow(() -> providerFailure);

        log.warn(
                "Using stale reference rate for {} fetched at {}",
                pair.symbol(),
                fallbackRate.fetchedAt()
        );

        log.debug(
                "Reference rate provider failure details",
                providerFailure
        );

        return ResolvedReferenceRate.stale(
                fallbackRate
        );
    }
}
