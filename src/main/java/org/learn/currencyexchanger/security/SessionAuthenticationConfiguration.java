package org.learn.currencyexchanger.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SessionAuthenticationConfiguration {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository
    ) {
        ChangeSessionIdAuthenticationStrategy sessionFixationProtection =
                new ChangeSessionIdAuthenticationStrategy();

        CsrfAuthenticationStrategy csrfAuthenticationStrategy =
                new CsrfAuthenticationStrategy(csrfTokenRepository);

        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        sessionFixationProtection,
                        csrfAuthenticationStrategy
                )
        );
    }
}
