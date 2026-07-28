package org.learn.currencyexchanger.rate.application;

import org.learn.currencyexchanger.rate.domain.CurrencyPair;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;


//Projekt nie wykorzystuje redis
//Dziala w obrebie jednej instancji JVM
//Przy wielu kopiach dziala we wlasnych odswiezaniach
public final class ReferenceRateRefreshCoordinator {

    private final ConcurrentMap<
            CurrencyPair,
            FutureTask<ReferenceRateSnapshot>
            >
            refreshesInProgress = new ConcurrentHashMap<>();

    private static ReferenceRateSnapshot await(
            FutureTask<ReferenceRateSnapshot> refresh
    ) {
        try {
            return refresh.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for " +
                            "reference rate refresh",
                    exception
            );
        } catch (ExecutionException exception) {
            throw propagate(exception.getCause());
        }
    }

    private static RuntimeException propagate(
            Throwable cause
    ) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        if (cause instanceof Error error) {
            throw error;
        }

        return new IllegalStateException(
                "Reference rate refresh failed",
                cause
        );
    }

    public ReferenceRateSnapshot execute(
            CurrencyPair pair,
            Supplier<ReferenceRateSnapshot> refreshAction
    ) {
        Objects.requireNonNull(
                pair,
                "Currency pair cannot be null"
        );

        Objects.requireNonNull(
                refreshAction,
                "Refresh action cannot be null"
        );

        FutureTask<ReferenceRateSnapshot> candidate =
                new FutureTask<>(
                        () -> Objects.requireNonNull(
                                refreshAction.get(),
                                "Refresh result cannot be null"
                        )
                );

        FutureTask<ReferenceRateSnapshot> existing =
                refreshesInProgress.putIfAbsent(
                        pair,
                        candidate
                );

        if (existing != null) {
            return await(existing);
        }

        try {
            // Akcja wykonuje sie w watku pierwszego zadania
            candidate.run();
            return await(candidate);
        } finally {
            refreshesInProgress.remove(
                    pair,
                    candidate
            );
        }
    }
}
