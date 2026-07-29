package org.learn.currencyexchanger.rate.api.problem;

import jakarta.servlet.http.HttpServletRequest;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.exception.ReferenceRateNotFoundException;
import org.learn.currencyexchanger.rate.application.exception.UnsupportedCurrencyException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class RateApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RateApiExceptionHandler.class
            );

    private final ApiProblemFactory apiProblemFactory;

    public RateApiExceptionHandler(
            ApiProblemFactory apiProblemFactory
    ) {
        this.apiProblemFactory = apiProblemFactory;
    }

    @ExceptionHandler(InvalidCurrencyCodeException.class)
    public ProblemDetail handleInvalidCurrencyCode(
            InvalidCurrencyCodeException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.INVALID_CURRENCY_CODE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCurrencyPairException.class)
    public ProblemDetail handleInvalidCurrencyPair(
            InvalidCurrencyPairException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.INVALID_CURRENCY_PAIR,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrency(
            UnsupportedCurrencyException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.UNSUPPORTED_CURRENCY,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ReferenceRateNotFoundException.class)
    public ProblemDetail handleReferenceRateNotFound(
            ReferenceRateNotFoundException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.REFERENCE_RATE_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(RateProviderUnavailableException.class)
    public ProblemDetail handleRateProviderUnavailable(
            RateProviderUnavailableException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Reference rate provider unavailable while handling {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        log.debug(
                "Reference rate provider failure details",
                exception
        );

        return apiProblemFactory.create(
                ApiProblemCode.RATE_PROVIDER_UNAVAILABLE,
                request
        );
    }

    @ExceptionHandler(InvalidRateProviderResponseException.class)
    public ProblemDetail handleInvalidRateProviderResponse(
            InvalidRateProviderResponseException exception,
            HttpServletRequest request
    ) {
        log.error(
                "Reference rate provider returned an invalid response while handling {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return apiProblemFactory.create(
                ApiProblemCode.INVALID_RATE_PROVIDER_RESPONSE,
                request
        );
    }

    @ExceptionHandler(InvalidMoneyAmountException.class)
    public ProblemDetail handleInvalidMoneyAmount(
            InvalidMoneyAmountException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.INVALID_MONEY_AMOUNT,
                exception.getMessage(),
                request
        );
    }

}
