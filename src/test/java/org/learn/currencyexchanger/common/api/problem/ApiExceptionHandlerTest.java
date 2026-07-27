package org.learn.currencyexchanger.common.api.problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ApiExceptionHandlerTest.TestController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        ApiExceptionHandler.class,
        ApiProblemFactory.class,
        ApiExceptionHandlerTest.TestController.class
})
class ApiExceptionHandlerTest {

    private static final String BASE_PATH =
            "/api/test/problems";

    private final MockMvc mockMvc;

    @Autowired
    ApiExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static Stream<Arguments> mappedProblems() {
        return Stream.of(
                Arguments.of(
                        "user-not-found",
                        ApiProblemCode.USER_NOT_FOUND,
                        ApiProblemCode.USER_NOT_FOUND.defaultDetail()
                ),
                Arguments.of(
                        "username-already-used",
                        ApiProblemCode.USERNAME_ALREADY_USED,
                        ApiProblemCode.USERNAME_ALREADY_USED.defaultDetail()
                ),
                Arguments.of(
                        "invalid-username",
                        ApiProblemCode.INVALID_USERNAME,
                        "Username contains unsupported characters"
                ),
                Arguments.of(
                        "data-conflict",
                        ApiProblemCode.DATA_CONFLICT,
                        ApiProblemCode.DATA_CONFLICT.defaultDetail()
                ),
                Arguments.of(
                        "concurrent-modification",
                        ApiProblemCode.CONCURRENT_MODIFICATION,
                        ApiProblemCode.CONCURRENT_MODIFICATION
                                .defaultDetail()
                ),
                Arguments.of(
                        "authentication-required",
                        ApiProblemCode.AUTHENTICATION_REQUIRED,
                        ApiProblemCode.AUTHENTICATION_REQUIRED
                                .defaultDetail()
                ),
                Arguments.of(
                        "invalid-csrf",
                        ApiProblemCode.INVALID_CSRF_TOKEN,
                        ApiProblemCode.INVALID_CSRF_TOKEN.defaultDetail()
                ),
                Arguments.of(
                        "access-denied",
                        ApiProblemCode.ACCESS_DENIED,
                        ApiProblemCode.ACCESS_DENIED.defaultDetail()
                ),
                Arguments.of(
                        "disabled-user",
                        ApiProblemCode.USER_STATE_CONFLICT,
                        "Disabled user cannot be modified"
                ),
                Arguments.of(
                        "cannot-unlock",
                        ApiProblemCode.USER_STATE_CONFLICT,
                        "User with status ACTIVE cannot be unlocked"
                ),
                Arguments.of(
                        "invalid-password",
                        ApiProblemCode.INVALID_PASSWORD,
                        "Password must contain at least 12 characters"
                ),
                Arguments.of(
                        "authentication-failed",
                        ApiProblemCode.AUTHENTICATION_FAILED,
                        ApiProblemCode.AUTHENTICATION_FAILED
                                .defaultDetail()
                ),
                Arguments.of(
                        "unexpected",
                        ApiProblemCode.INTERNAL_ERROR,
                        ApiProblemCode.INTERNAL_ERROR.defaultDetail()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("mappedProblems")
    void shouldMapApplicationDomainAndSecurityExceptions(
            String caseName,
            ApiProblemCode expectedCode,
            String expectedDetail
    ) throws Exception {
        String path = BASE_PATH + "/mapped/" + caseName;

        mockMvc.perform(
                        get(path)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().is(
                        expectedCode.status().value()
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value(
                        expectedCode.type().toString()
                ))
                .andExpect(jsonPath("$.title").value(
                        expectedCode.title()
                ))
                .andExpect(jsonPath("$.status").value(
                        expectedCode.status().value()
                ))
                .andExpect(jsonPath("$.detail").value(
                        expectedDetail
                ))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.code").value(
                        expectedCode.name()
                ));
    }

    @Test
    void shouldReturnValidationProblemForInvalidRequestBody()
            throws Exception {
        mockMvc.perform(
                        post(BASE_PATH + "/validated")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .content("""
                                        {
                                          "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.instance")
                        .value(BASE_PATH + "/validated"))
                .andExpect(jsonPath("$.violations.length()")
                        .value(1))
                .andExpect(jsonPath("$.violations[0].path")
                        .value("name"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("Name is required"));
    }

    @Test
    void shouldReturnMalformedRequestForInvalidJson()
            throws Exception {
        mockMvc.perform(
                        post(BASE_PATH + "/validated")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .content("""
                                        {
                                          "name":
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value(
                        ApiProblemCode.MALFORMED_REQUEST
                                .defaultDetail()
                ));
    }

    @Test
    void shouldReturnInvalidParameterWhenParameterIsMissing()
            throws Exception {
        mockMvc.perform(
                        get(BASE_PATH + "/parameter")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.detail").value(
                        "The required request parameter 'limit' is missing"
                ));
    }

    @Test
    void shouldReturnInvalidParameterForTypeMismatch()
            throws Exception {
        mockMvc.perform(
                        get(BASE_PATH + "/parameter")
                                .param("limit", "not-a-number")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void shouldReturnValidationProblemForInvalidMethodParameter()
            throws Exception {
        mockMvc.perform(
                        get(BASE_PATH + "/parameter")
                                .param("limit", "0")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.length()")
                        .value(1))
                .andExpect(jsonPath("$.violations[0].path")
                        .value("limit"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("Limit must be at least 1"));
    }

    @Test
    void shouldReturnMethodNotAllowedProblem()
            throws Exception {
        mockMvc.perform(
                        put(BASE_PATH + "/validated")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .content("""
                                        {
                                          "name": "value"
                                        }
                                        """)
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code")
                        .value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void shouldReturnUnsupportedMediaTypeProblem()
            throws Exception {
        mockMvc.perform(
                        post(BASE_PATH + "/validated")
                                .contentType(MediaType.TEXT_PLAIN)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .content("name=value")
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code")
                        .value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void shouldReturnResourceNotFoundProblem()
            throws Exception {
        String path = BASE_PATH + "/missing/resource";

        mockMvc.perform(
                        get(path)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.instance")
                        .value(path));
    }

    @RestController
    @RequestMapping(BASE_PATH)
    public static class TestController {

        @GetMapping("/mapped/{caseName}")
        void throwMappedException(
                @PathVariable String caseName
        ) {
            throw switch (caseName) {
                case "user-not-found" -> new UserNotFoundException(
                        UUID.fromString(
                                "00000000-0000-0000-0000-000000000001"
                        )
                );
                case "username-already-used" -> new UsernameAlreadyUsedException();
                case "invalid-username" -> new InvalidUsernameException(
                        "Username contains unsupported characters"
                );
                case "data-conflict" -> new DataIntegrityViolationException(
                        "Sensitive database details"
                );
                case "concurrent-modification" -> new OptimisticLockingFailureException(
                        "Optimistic lock failed"
                );
                case "authentication-required" -> new InsufficientAuthenticationException(
                        "Full authentication is required"
                );
                case "invalid-csrf" -> new MissingCsrfTokenException(
                        "Actual internal CSRF message"
                );
                case "access-denied" -> new AccessDeniedException(
                        "Internal authorization details"
                );
                case "disabled-user" -> new DisabledUserCannotBeModifiedException();
                case "cannot-unlock" -> new UserCannotBeUnlockedException(
                        UserStatus.ACTIVE
                );
                case "invalid-password" -> new InvalidPasswordException(
                        "Password must contain at least 12 characters"
                );
                case "authentication-failed" -> new BadCredentialsException(
                        "User john.doe does not exist"
                );
                case "unexpected" -> new IllegalStateException(
                        "Sensitive implementation details"
                );
                default -> new IllegalArgumentException(
                        "Unknown test case"
                );
            };
        }

        @PostMapping(
                value = "/validated",
                consumes = MediaType.APPLICATION_JSON_VALUE
        )
        void validateRequest(
                @Valid @RequestBody TestRequest request
        ) {
        }

        @GetMapping("/parameter")
        void validateParameter(
                @RequestParam
                @Min(
                        value = 1,
                        message = "Limit must be at least 1"
                )
                int limit
        ) {
        }
    }

    record TestRequest(
            @NotBlank(message = "Name is required")
            String name
    ) {
    }
}
