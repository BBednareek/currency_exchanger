package org.learn.currencyexchanger.rate.configuration;

import org.learn.currencyexchanger.rate.application.ReferenceRateCachePolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReferenceRateCacheProperties.class)
public class ReferenceRateCacheConfiguration {

    @Bean
    public ReferenceRateCachePolicy referenceRateCachePolicy(
            ReferenceRateCacheProperties referenceRateCacheProperties
    ) {
        return new ReferenceRateCachePolicy(
                referenceRateCacheProperties.timeToLive(),
                referenceRateCacheProperties.maximumFallbackAge()
        );
    }
}
