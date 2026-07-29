package org.learn.currencyexchanger.security.configuration;

import jakarta.servlet.DispatcherType;
import org.learn.currencyexchanger.security.api.SecurityExceptionResolverBridge;
import org.learn.currencyexchanger.security.auth.ActiveAccountSessionFilter;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public ActiveAccountSessionFilter activeAccountSessionFilter(
            UserRepository userRepository,
            LogoutHandler logoutHandler,
            SecurityExceptionResolverBridge securityExceptionResolverBridge
    ) {
        return new ActiveAccountSessionFilter(
                userRepository,
                logoutHandler,
                securityExceptionResolverBridge
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            SecurityExceptionResolverBridge securityExceptionResolverBridge,
            ActiveAccountSessionFilter activeAccountSessionFilter
    ) {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(
                                securityContextRepository
                        )
                        .requireExplicitSave(true)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                securityExceptionResolverBridge
                        )
                        .accessDeniedHandler(
                                securityExceptionResolverBridge
                        )
                )
                .addFilterAfter(activeAccountSessionFilter, CsrfFilter.class)
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
