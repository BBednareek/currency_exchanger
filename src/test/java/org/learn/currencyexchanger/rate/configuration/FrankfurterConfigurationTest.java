package org.learn.currencyexchanger.rate.configuration;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.infrastructure.FrankfurterReferenceRateProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FrankfurterConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(
                            RestClient.Builder.class,
                            RestClient::builder
                    )
                    .withBean(
                            Clock.class,
                            Clock::systemUTC
                    )
                    .withUserConfiguration(
                            FrankfurterConfiguration.class
                    )
                    .withPropertyValues(
                            "frankfurter.base-url="
                                    + "https://api.frankfurter.dev/v2",
                            "frankfurter.connect-timeout=2s",
                            "frankfurter.read-timeout=3s"
                    );

    @Test
    void shouldBindPropertiesAndCreateRestClient() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());

            FrankfurterProperties properties =
                    context.getBean(
                            FrankfurterProperties.class
                    );

            RestClient restClient =
                    context.getBean(
                            "frankfurterRestClient",
                            RestClient.class
                    );

            ReferenceRateProvider referenceRateProvider =
                    context.getBean(ReferenceRateProvider.class);


            assertNotNull(restClient);

            assertInstanceOf(
                    FrankfurterReferenceRateProvider.class,
                    referenceRateProvider
            );

            assertEquals(
                    URI.create(
                            "https://api.frankfurter.dev/v2"
                    ),
                    properties.baseUrl()
            );
            assertEquals(
                    Duration.ofSeconds(2),
                    properties.connectTimeout()
            );
            assertEquals(
                    Duration.ofSeconds(3),
                    properties.readTimeout()
            );
        });
    }

    @Test
    void shouldRejectInsecureBaseUrlDuringStartup() {
        contextRunner
                .withPropertyValues(
                        "frankfurter.base-url="
                                + "http://api.frankfurter.dev/v2"
                )
                .run(context ->
                        assertNotNull(
                                context.getStartupFailure()
                        )
                );
    }

    @Test
    void shouldRejectNonPositiveTimeoutDuringStartup() {
        contextRunner
                .withPropertyValues(
                        "frankfurter.read-timeout=0s"
                )
                .run(context ->
                        assertNotNull(
                                context.getStartupFailure()
                        )
                );
    }
}
