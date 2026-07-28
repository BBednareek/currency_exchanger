package org.learn.currencyexchanger.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class SessionLogoutConfiguration {
    @Bean
    public LogoutHandler securityContextLogoutHandler(
            SecurityContextRepository securityContextRepository
    ) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

        logoutHandler.setSecurityContextRepository(securityContextRepository);
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);

        return logoutHandler;
    }
}
