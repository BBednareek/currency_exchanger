package org.learn.currencyexchanger.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class PasswordEncodingConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt =
                new BCryptPasswordEncoder();

        DelegatingPasswordEncoder delegating =
                new DelegatingPasswordEncoder(
                        "bcrypt",
                        Map.of("bcrypt", bcrypt)
                );

        delegating.setDefaultPasswordEncoderForMatches(bcrypt);

        return delegating;
    }
}
