package org.learn.currencyexchanger.rate.configuration;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.application.ReferenceRateCachePolicy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReferenceRateCacheConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            ReferenceRateCacheConfiguration.class
                    )
                    .withPropertyValues(
                            "reference-rate.cache.time-to-live=1h",
                            "reference-rate.cache.maximum-fallback-age=168h"
                    );

    @Test
    void shouldBindPropertiesAndCreateCachePolicy() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());

            ReferenceRateCacheProperties properties =
                    context.getBean(
                            ReferenceRateCacheProperties.class
                    );

            ReferenceRateCachePolicy policy =
                    context.getBean(
                            ReferenceRateCachePolicy.class
                    );

            assertNotNull(policy);

            assertEquals(
                    Duration.ofHours(1),
                    properties.timeToLive()
            );

            assertEquals(
                    Duration.ofDays(7),
                    properties.maximumFallbackAge()
            );
        });
    }

    @Test
    void shouldRejectNonPositiveTimeToLive() {
        contextRunner
                .withPropertyValues(
                        "reference-rate.cache.time-to-live=0s"
                )
                .run(context ->
                        assertNotNull(
                                context.getStartupFailure()
                        )
                );
    }

    @Test
    void shouldRejectFallbackAgeShorterThanTimeToLive() {
        contextRunner
                .withPropertyValues(
                        "reference-rate.cache.time-to-live=2h",
                        "reference-rate.cache.maximum-fallback-age=1h"
                )
                .run(context ->
                        assertNotNull(
                                context.getStartupFailure()
                        )
                );
    }
}
