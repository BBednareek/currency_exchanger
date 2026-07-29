package org.learn.currencyexchanger.auth.api.problem;

import jakarta.servlet.http.HttpServletRequest;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class AuthApiExceptionHandler {
    private final ApiProblemFactory apiProblemFactory;

    public AuthApiExceptionHandler(
            ApiProblemFactory apiProblemFactory
    ) {
        this.apiProblemFactory = apiProblemFactory;
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(
            InvalidPasswordException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.INVALID_PASSWORD,
                exception.getMessage(),
                request
        );
    }
}
