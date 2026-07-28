package org.learn.currencyexchanger.common.api.problem;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum ApiProblemCode {

    USER_NOT_FOUND(
            "user-not-found",
            HttpStatus.NOT_FOUND,
            "User not found",
            "The requested user does not exist"
    ),
    USERNAME_ALREADY_USED(
            "username-already-used",
            HttpStatus.CONFLICT,
            "Username already used",
            "Username is already used"
    ),
    INVALID_USERNAME(
            "invalid-username",
            HttpStatus.BAD_REQUEST,
            "Invalid username",
            "The username does not satisfy the required format"
    ),
    INVALID_PASSWORD(
            "invalid-password",
            HttpStatus.BAD_REQUEST,
            "Invalid password",
            "The password does not satisfy the security policy"
    ),
    VALIDATION_FAILED(
            "validation-failed",
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            "One or more request fields contain invalid values"
    ),
    MALFORMED_REQUEST(
            "malformed-request",
            HttpStatus.BAD_REQUEST,
            "Malformed request",
            "The request body is missing or contains malformed JSON"
    ),
    INVALID_REQUEST_PARAMETER(
            "invalid-request-parameter",
            HttpStatus.BAD_REQUEST,
            "Invalid request parameter",
            "One or more request parameters are invalid"
    ),
    AUTHENTICATION_REQUIRED(
            "authentication-required",
            HttpStatus.UNAUTHORIZED,
            "Authentication required",
            "Authentication is required to access this resource"
    ),
    INVALID_MONEY_AMOUNT(
            "invalid-money-amount",
            HttpStatus.BAD_REQUEST,
            "Invalid money amount",
            "Money amount must be greater than zero"
    ),
    AUTHENTICATION_FAILED(
            "authentication-failed",
            HttpStatus.UNAUTHORIZED,
            "Authentication failed",
            "Invalid username or password"
    ),
    ACCESS_DENIED(
            "access-denied",
            HttpStatus.FORBIDDEN,
            "Access denied",
            "You do not have permission to access this resource"
    ),
    INVALID_CSRF_TOKEN(
            "invalid-csrf-token",
            HttpStatus.FORBIDDEN,
            "Invalid CSRF token",
            "The CSRF token is missing or invalid"
    ),
    RESOURCE_NOT_FOUND(
            "resource-not-found",
            HttpStatus.NOT_FOUND,
            "Resource not found",
            "The requested API resource does not exist"
    ),
    METHOD_NOT_ALLOWED(
            "method-not-allowed",
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method not allowed",
            "The HTTP method is not supported for this resource"
    ),
    UNSUPPORTED_MEDIA_TYPE(
            "unsupported-media-type",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported media type",
            "The request content type is not supported"
    ),
    DATA_CONFLICT(
            "data-conflict",
            HttpStatus.CONFLICT,
            "Data conflict",
            "The request conflicts with the current state of the resource"
    ),
    CONCURRENT_MODIFICATION(
            "concurrent-modification",
            HttpStatus.CONFLICT,
            "Concurrent modification",
            "The resource was modified by another request. Reload it and try again"
    ),
    USER_STATE_CONFLICT(
            "user-state-conflict",
            HttpStatus.CONFLICT,
            "User state conflict",
            "The requested operation is not allowed for the current user state"
    ),

    INVALID_CURRENCY_CODE(
            "invalid-currency-code",
            HttpStatus.BAD_REQUEST,
            "Invalid currency code",
            "Currency code must contain exactly three ASCII letters"
    ),
    INVALID_CURRENCY_PAIR(
            "invalid-currency-pair",
            HttpStatus.BAD_REQUEST,
            "Invalid currency pair",
            "Base and quote currencies must be different"
    ),
    UNSUPPORTED_CURRENCY(
            "unsupported-currency",
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Unsupported currency",
            "The requested currency pair is not supported"
    ),
    REFERENCE_RATE_NOT_FOUND(
            "reference-rate-not-found",
            HttpStatus.NOT_FOUND,
            "Reference rate not found",
            "A reference rate was not found for the requested currency pair"
    ),
    INVALID_RATE_PROVIDER_RESPONSE(
            "invalid-rate-provider-response",
            HttpStatus.BAD_GATEWAY,
            "Invalid provider response",
            "The reference rate provider returned an invalid response"
    ),
    RATE_PROVIDER_UNAVAILABLE(
            "rate-provider-unavailable",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Rate provider unavailable",
            "The reference rate provider is temporarily unavailable"
    ),
    INTERNAL_ERROR(
            "internal-error",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred"
    );

    private static final String TYPE_PREFIX = "urn:problem:";

    private final URI type;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    ApiProblemCode(
            String typeSlug,
            HttpStatus status,
            String title,
            String defaultDetail
    ) {
        this.type = URI.create(TYPE_PREFIX + typeSlug);
        this.status = status;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

    public URI type() {
        return type;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String defaultDetail() {
        return defaultDetail;
    }
}
