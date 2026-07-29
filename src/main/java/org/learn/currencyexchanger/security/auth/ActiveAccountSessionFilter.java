package org.learn.currencyexchanger.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.learn.currencyexchanger.security.api.SecurityExceptionResolverBridge;
import org.learn.currencyexchanger.user.application.port.AccountStatusReader;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class ActiveAccountSessionFilter extends
        OncePerRequestFilter {

    private final AccountStatusReader accountStatusReader;
    private final LogoutHandler logoutHandler;
    private final SecurityExceptionResolverBridge exceptionHandler;

    public ActiveAccountSessionFilter(
            AccountStatusReader accountStatusReader,
            LogoutHandler logoutHandler,
            SecurityExceptionResolverBridge exceptionHandler
    ) {
        this.accountStatusReader = accountStatusReader;
        this.logoutHandler = logoutHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AppUserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean accountIsActive = accountStatusReader.isActive(
                principal.getUserId()
        );

        if (accountIsActive) {
            filterChain.doFilter(request, response);
            return;
        }

        logoutHandler.logout(
                request,
                response,
                authentication
        );

        exceptionHandler.commence(
                request,
                response,
                new InsufficientAuthenticationException(
                        "Session is no longer valid"
                )
        );
    }
}
