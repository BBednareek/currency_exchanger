package org.learn.currencyexchanger.security.api.problem;

import jakarta.servlet.http.HttpServletRequest;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class SecurityApiExceptionHandler {

    private final ApiProblemFactory problemFactory;

    public SecurityApiExceptionHandler(
            ApiProblemFactory problemFactory
    ) {
        this.problemFactory = problemFactory;
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ProblemDetail handleAuthenticationRequired(
            InsufficientAuthenticationException exception,
            HttpServletRequest request
    ) {
        return problemFactory.create(
                ApiProblemCode.AUTHENTICATION_REQUIRED,
                request
        );
    }

    @ExceptionHandler(CsrfException.class)
    public ProblemDetail handleCsrfFailure(
            CsrfException exception,
            HttpServletRequest request
    ) {
        return problemFactory.create(
                ApiProblemCode.INVALID_CSRF_TOKEN,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return problemFactory.create(
                ApiProblemCode.ACCESS_DENIED,
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return problemFactory.create(
                ApiProblemCode.AUTHENTICATION_FAILED,
                request
        );
    }
}
