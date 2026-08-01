package org.learn.currencyexchanger.security.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PasswordEncodingProperties.class)
public final class PasswordEncodingConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder(
            PasswordEncodingProperties properties
    ) {
        BCryptPasswordEncoder bcrypt =
                new BCryptPasswordEncoder(
                        properties.bcryptStrength()
                );

        return new DelegatingPasswordEncoder(
                "bcrypt",
                Map.of(
                        "bcrypt",
                        bcrypt
                )
        );
    }
}
