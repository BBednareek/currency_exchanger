package org.learn.currencyexchanger.common.api;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.naming.AuthenticationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final String USERNAME_ALREADY_USED = "USERNAME_ALREADY_USED";
    private static final String INVALID_USERNAME = "INVALID_USERNAME";
    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String DATA_CONFLICT = "DATA_CONFLICT";
    private static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String USER_STATE_CONFLICT = "USER_STATE_CONFLICT";
    private static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    private static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    private static ProblemDetail createProblem(
            HttpStatus status,
            String code,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);
        problem.setType(URI.create("urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);

        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                USER_NOT_FOUND,
                "User not found",
                "The requested user does not exist"
        );
    }

    @ExceptionHandler(UsernameAlreadyUsedException.class)
    public ProblemDetail handleUsernameAlreadyUsed(UsernameAlreadyUsedException exception) {
        return createProblem(
                HttpStatus.CONFLICT,
                USERNAME_ALREADY_USED,
                "Username already used",
                exception.getMessage()
        );
    }

    //Ochrona przez race condition
    // Dwa zadajnia moga jednoczesnie przejsc existsByUsername()
    // a dopiero ograncizenie unique w bazie rozstrzygnie konflikt

    @ExceptionHandler(InvalidUsernameException.class)
    public ProblemDetail handleInvalidUsername(InvalidUsernameException exception) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                INVALID_USERNAME,
                "Invalid username",
                exception.getMessage()
        );
    }

    // Encja user korzysta z @Version dlatego rownolegla aktualizacja moze zakonczyc sie wyjatkiem optimistic lock

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Database integrity constraint was violated", exception);

        return createProblem(
                HttpStatus.CONFLICT,
                DATA_CONFLICT,
                "Data conflict",
                "The request conflicts with the current state of the resource"
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
        return createProblem(
                HttpStatus.CONFLICT,
                CONCURRENT_MODIFICATION,
                "Concurrent modification",
                "The resource was modified by another request. Reload it and try again"
        );
    }

    //Obsluga bledow bean validation
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        Stream<ValidationViolation> fieldViolations =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ValidationViolation(
                                error.getField(),
                                Objects.requireNonNullElse(
                                        error.getDefaultMessage(),
                                        "Invalid value"
                                )
                        ));

        Stream<ValidationViolation> objectViolations =
                exception.getBindingResult()
                        .getGlobalErrors()
                        .stream()
                        .map(error -> new ValidationViolation(
                                error.getObjectName(),
                                Objects.requireNonNullElse(
                                        error.getDefaultMessage(),
                                        "Invalid value"
                                )
                        ));

        List<ValidationViolation> violations = Stream
                .concat(fieldViolations, objectViolations)
                .toList();

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                "Request validation failed",
                "One or more request fields contain invalid values"
        );

        problem.setProperty("violations", violations);

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), exception);

        return createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR,
                "Internal server error",
                "An unexpected error occurred"
        );
    }

    @ExceptionHandler({
            DisabledUserCannotBeModifiedException.class,
            UserCannotBeUnlockedException.class
    })
    public ProblemDetail handleUserStateConflict(RuntimeException exception) {
        return createProblem(
                HttpStatus.CONFLICT,
                USER_STATE_CONFLICT,
                "User state conflict",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(
            InvalidPasswordException exception
    ) {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                INVALID_PASSWORD,
                "Invalid password",
                exception.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException exception) {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                AUTHENTICATION_FAILED,
                "Authentication failed",
                "Invalid username or password"
        );
    }

    public record ValidationViolation(
            String path,
            String message
    ) {
    }
}
