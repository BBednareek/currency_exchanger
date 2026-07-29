package org.learn.currencyexchanger.user.api.problem;

import jakarta.servlet.http.HttpServletRequest;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.user.application.exception.ConcurrentUserModificationException;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class UserApiExceptionHandler {
    private final ApiProblemFactory apiProblemFactory;

    public UserApiExceptionHandler(
            ApiProblemFactory apiProblemFactory
    ) {
        this.apiProblemFactory = apiProblemFactory;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.USER_NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(UsernameAlreadyUsedException.class)
    public ProblemDetail handleUsernameAlreadyUsed(
            UsernameAlreadyUsedException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.USERNAME_ALREADY_USED,
                request
        );
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ProblemDetail handleInvalidUsername(
            InvalidUsernameException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.INVALID_USERNAME,
                exception.getMessage(),
                request
        );
    }

    /*
     * Encja User używa @Version, dlatego równoległa aktualizacja
     * może skończyć się wyjątkiem optimistic locking.
     */
    @ExceptionHandler(ConcurrentUserModificationException.class)
    public ProblemDetail handleConcurrentUserModification(
            ConcurrentUserModificationException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.CONCURRENT_MODIFICATION,
                request
        );
    }

    @ExceptionHandler({
            DisabledUserCannotBeModifiedException.class,
            UserCannotBeUnlockedException.class
    })
    public ProblemDetail handleUserStateConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return apiProblemFactory.create(
                ApiProblemCode.USER_STATE_CONFLICT,
                exception.getMessage(),
                request
        );
    }
}
