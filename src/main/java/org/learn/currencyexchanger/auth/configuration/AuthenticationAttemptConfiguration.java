package org.learn.currencyexchanger.auth.configuration;

import org.learn.currencyexchanger.auth.domain.AuthenticationAttemptPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationAttemptProperties.class)
@EnableScheduling
public final class AuthenticationAttemptConfiguration {

    @Bean
    public AuthenticationAttemptPolicy authenticationAttemptPolicy(
            AuthenticationAttemptProperties properties
    ) {
        return new AuthenticationAttemptPolicy(
                properties.maximumFailures(),
                properties.failureWindow(),
                properties.blockDuration(),
                properties.retention()
        );
    }
}
