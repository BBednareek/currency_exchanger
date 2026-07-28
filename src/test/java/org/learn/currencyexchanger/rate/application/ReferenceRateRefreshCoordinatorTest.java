package org.learn.currencyexchanger.rate.application;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class ReferenceRateRefreshCoordinatorTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final ResolvedReferenceRate RESULT =
            ResolvedReferenceRate.fresh(
                    new ReferenceRate(
                            PAIR,
                            new BigDecimal("3.672100"),
                            LocalDate.of(2026, 7, 28),
                            Instant.parse(
                                    "2026-07-28T12:00:00Z"
                            )
                    )
            );

    private final ReferenceRateRefreshCoordinator coordinator =
            new ReferenceRateRefreshCoordinator();

    private static void awaitUnchecked(
            CountDownLatch latch
    ) {
        try {
            if (!latch.await(
                    5,
                    TimeUnit.SECONDS
            )) {
                throw new IllegalStateException(
                        "Timed out while waiting for test latch"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Test thread was interrupted",
                    exception
            );
        }
    }

    private static void awaitWaiting(
            List<AtomicReference<Thread>> threads
    ) {
        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            boolean allWaiting = threads.stream()
                    .map(AtomicReference::get)
                    .allMatch(thread ->
                            thread != null
                                    && isWaiting(thread)
                    );

            if (allWaiting) {
                return;
            }

            LockSupport.parkNanos(
                    TimeUnit.MILLISECONDS.toNanos(1)
            );
        }

        fail("Followers did not start waiting for refresh");
    }

    private static boolean isWaiting(
            Thread thread
    ) {
        return thread.getState() == Thread.State.WAITING
                || thread.getState()
                == Thread.State.TIMED_WAITING;
    }

    @Test
    void shouldShareSingleRefreshBetweenConcurrentCallers()
            throws Exception {
        int followerCount = 10;

        CountDownLatch refreshStarted =
                new CountDownLatch(1);

        CountDownLatch releaseRefresh =
                new CountDownLatch(1);

        CountDownLatch followersReady =
                new CountDownLatch(followerCount);

        CountDownLatch startFollowers =
                new CountDownLatch(1);

        AtomicInteger executionCount =
                new AtomicInteger();

        List<AtomicReference<Thread>> followerThreads =
                new ArrayList<>();

        try (
                ExecutorService executor =
                        Executors.newVirtualThreadPerTaskExecutor()
        ) {
            Future<ResolvedReferenceRate> owner =
                    executor.submit(() ->
                            coordinator.execute(
                                    PAIR,
                                    () -> {
                                        executionCount.incrementAndGet();
                                        refreshStarted.countDown();
                                        awaitUnchecked(releaseRefresh);

                                        return RESULT;
                                    }
                            )
                    );

            if (!refreshStarted.await(
                    5,
                    TimeUnit.SECONDS
            )) {
                fail("Refresh did not start");
            }

            List<Future<ResolvedReferenceRate>> followers =
                    new ArrayList<>();

            for (int index = 0;
                 index < followerCount;
                 index++) {

                AtomicReference<Thread> threadReference =
                        new AtomicReference<>();

                followerThreads.add(threadReference);

                followers.add(executor.submit(() -> {
                    threadReference.set(
                            Thread.currentThread()
                    );

                    followersReady.countDown();
                    awaitUnchecked(startFollowers);

                    return coordinator.execute(
                            PAIR,
                            () -> {
                                executionCount.incrementAndGet();

                                throw new AssertionError(
                                        "Follower must not execute refresh"
                                );
                            }
                    );
                }));
            }

            if (!followersReady.await(
                    5,
                    TimeUnit.SECONDS
            )) {
                fail("Followers were not ready");
            }

            startFollowers.countDown();

            awaitWaiting(followerThreads);
            releaseRefresh.countDown();

            assertSame(
                    RESULT,
                    owner.get(5, TimeUnit.SECONDS)
            );

            for (Future<ResolvedReferenceRate> follower
                    : followers) {
                assertSame(
                        RESULT,
                        follower.get(
                                5,
                                TimeUnit.SECONDS
                        )
                );
            }

            assertEquals(
                    1,
                    executionCount.get()
            );
        } finally {
            releaseRefresh.countDown();
            startFollowers.countDown();
        }
    }

    @Test
    void shouldRemoveFailedRefreshAndAllowRetry() {
        AtomicInteger executionCount =
                new AtomicInteger();

        RateProviderUnavailableException expected =
                new RateProviderUnavailableException();

        RateProviderUnavailableException result =
                assertThrows(
                        RateProviderUnavailableException.class,
                        () -> coordinator.execute(
                                PAIR,
                                () -> {
                                    executionCount.incrementAndGet();
                                    throw expected;
                                }
                        )
                );

        assertSame(expected, result);

        ResolvedReferenceRate retryResult =
                coordinator.execute(
                        PAIR,
                        () -> {
                            executionCount.incrementAndGet();
                            return RESULT;
                        }
                );

        assertSame(RESULT, retryResult);
        assertEquals(2, executionCount.get());
    }
}
