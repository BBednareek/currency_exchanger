package org.learn.currencyexchanger.security.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public final class ApiSecurityExceptionHandler implements
        AuthenticationEntryPoint,
        AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiSecurityExceptionHandler(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException exception
    ) {
        resolveException(
                request,
                response,
                exception,
                HttpServletResponse.SC_UNAUTHORIZED
        );
    }

    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AccessDeniedException exception
    ) {
        resolveException(
                request,
                response,
                exception,
                HttpServletResponse.SC_FORBIDDEN
        );
    }

    private void resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception,
            int fallbackStatus
    ) {
        ModelAndView resolved = exceptionResolver.resolveException(
                request,
                response,
                null,
                exception
        );

        if (resolved == null && !response.isCommitted()) {
            response.setStatus(fallbackStatus);
        }
    }
}