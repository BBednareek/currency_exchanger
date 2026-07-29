package org.learn.currencyexchanger.common.api.problem;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public final class FrameworkApiExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    FrameworkApiExceptionHandler.class
            );

    private static final String DEFAULT_VALIDATION_MESSAGE =
            "Invalid value";

    private final ApiProblemFactory apiProblemFactory;

    public FrameworkApiExceptionHandler(
            ApiProblemFactory apiProblemFactory
    ) {
        this.apiProblemFactory = apiProblemFactory;
    }

    private static String parameterPath(
            ParameterValidationResult result
    ) {
        String parameterName = result
                .getMethodParameter()
                .getParameterName();

        if (parameterName != null) {
            return parameterName;
        }

        return "argument[%d]".formatted(
                result.getMethodParameter().getParameterIndex()
        );
    }

    private static String message(
            MessageSourceResolvable error
    ) {
        return Objects.requireNonNullElse(
                error.getDefaultMessage(),
                DEFAULT_VALIDATION_MESSAGE
        );
    }

    private static URI requestUri(WebRequest request) {
        if (!(request instanceof ServletWebRequest servletRequest)) {
            throw new IllegalStateException(
                    "ServletWebRequest was expected"
            );
        }

        return requestUri(servletRequest.getRequest());
    }

    private static URI requestUri(HttpServletRequest request) {
        /*
         * Celowo bez query string.
         * Parametry zapytania mogą zawierać dane, których nie należy
         * powielać w odpowiedzi błędu.
         */
        return URI.create(request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        List<ValidationViolation> fieldViolations =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ValidationViolation(
                                error.getField(),
                                message(error)
                        ))
                        .toList();

        List<ValidationViolation> objectViolations =
                exception.getBindingResult()
                        .getGlobalErrors()
                        .stream()
                        .map(error -> new ValidationViolation(
                                error.getObjectName(),
                                message(error)
                        ))
                        .toList();

        return validationResponse(
                exception,
                Stream.concat(
                                fieldViolations.stream(),
                                objectViolations.stream()
                        )
                        .toList(),
                headers,
                request
        );
    }

    /*
     * Obsługa walidacji parametrów kontrolera, np.:
     *
     * get(@RequestParam @Min(1) int page)
     *
     * Jest to inny mechanizm niż @Valid @RequestBody.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        Stream<ValidationViolation> parameterViolations =
                exception.getParameterValidationResults()
                        .stream()
                        .flatMap(result ->
                                result.getResolvableErrors()
                                        .stream()
                                        .map(error ->
                                                new ValidationViolation(
                                                        parameterPath(result),
                                                        message(error)
                                                )
                                        )
                        );

        Stream<ValidationViolation> crossParameterViolations =
                exception.getCrossParameterValidationResults()
                        .stream()
                        .map(error -> new ValidationViolation(
                                "request",
                                message(error)
                        ));

        return validationResponse(
                exception,
                Stream.concat(
                                parameterViolations,
                                crossParameterViolations
                        )
                        .toList(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.MALFORMED_REQUEST,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String detail = "The required request parameter '%s' is missing"
                .formatted(exception.getParameterName());

        return frameworkProblem(
                exception,
                ApiProblemCode.INVALID_REQUEST_PARAMETER,
                detail,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.INVALID_REQUEST_PARAMETER,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.METHOD_NOT_ALLOWED,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.UNSUPPORTED_MEDIA_TYPE,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.RESOURCE_NOT_FOUND,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return frameworkProblem(
                exception,
                ApiProblemCode.RESOURCE_NOT_FOUND,
                headers,
                request
        );
    }

    /*
     * Poniższe wyjątki oznaczają problem po stronie serwera,
     * a nie błędne żądanie klienta.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        log.error("Failed to write an HTTP response", exception);

        return frameworkProblem(
                exception,
                ApiProblemCode.INTERNAL_ERROR,
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(
            ConversionNotSupportedException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        log.error("Server-side value conversion failed", exception);

        return frameworkProblem(
                exception,
                ApiProblemCode.INTERNAL_ERROR,
                headers,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while handling {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return apiProblemFactory.create(
                ApiProblemCode.INTERNAL_ERROR,
                request
        );
    }

    private ResponseEntity<Object> validationResponse(
            Exception exception,
            List<ValidationViolation> violations,
            HttpHeaders headers,
            WebRequest request
    ) {
        List<ValidationViolation> normalizedViolations =
                violations.stream()
                        .distinct()
                        .sorted(
                                Comparator.comparing(
                                                ValidationViolation::path
                                        )
                                        .thenComparing(
                                                ValidationViolation::message
                                        )
                        )
                        .toList();

        ProblemDetail problem =
                apiProblemFactory.createValidationProblem(
                        normalizedViolations,
                        requestUri(request)
                );

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                ApiProblemCode.VALIDATION_FAILED.status(),
                request
        );
    }

    private ResponseEntity<Object> frameworkProblem(
            Exception exception,
            ApiProblemCode code,
            HttpHeaders headers,
            WebRequest request
    ) {
        ProblemDetail problem = apiProblemFactory.create(
                code,
                requestUri(request)
        );

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                code.status(),
                request
        );
    }

    private ResponseEntity<Object> frameworkProblem(
            Exception exception,
            ApiProblemCode code,
            String detail,
            HttpHeaders headers,
            WebRequest request
    ) {
        ProblemDetail problem = apiProblemFactory.create(
                code,
                detail,
                requestUri(request)
        );

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                code.status(),
                request
        );
    }
}
