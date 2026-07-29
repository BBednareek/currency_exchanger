package org.learn.currencyexchanger.security.api.problem;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                SecurityApiExceptionHandlerTest.TestController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        SecurityApiExceptionHandler.class,
        ApiProblemFactory.class,
        SecurityApiExceptionHandlerTest.TestController.class
})
class SecurityApiExceptionHandlerTest {

    private static final String BASE_PATH =
            "/api/test/problems/security";

    private final MockMvc mockMvc;

    @Autowired
    SecurityApiExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static Stream<Arguments> mappedProblems() {
        return Stream.of(
                Arguments.of(
                        "authentication-required",
                        ApiProblemCode.AUTHENTICATION_REQUIRED
                ),
                Arguments.of(
                        "authentication-failed",
                        ApiProblemCode.AUTHENTICATION_FAILED
                ),
                Arguments.of(
                        "invalid-csrf",
                        ApiProblemCode.INVALID_CSRF_TOKEN
                ),
                Arguments.of(
                        "access-denied",
                        ApiProblemCode.ACCESS_DENIED
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappedProblems")
    void shouldMapSecurityExceptionToProblemDetail(
            String caseName,
            ApiProblemCode expectedCode
    ) throws Exception {
        String path = BASE_PATH + "/" + caseName;

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
                        expectedCode.defaultDetail()
                ))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.code").value(
                        expectedCode.name()
                ))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @RestController
    @RequestMapping(BASE_PATH)
    public static class TestController {

        @GetMapping("/{caseName}")
        void throwMappedException(
                @PathVariable String caseName
        ) {
            throw switch (caseName) {
                case "authentication-required" -> new InsufficientAuthenticationException(
                        "Sensitive authentication details"
                );

                case "authentication-failed" -> new BadCredentialsException(
                        "User john.doe does not exist"
                );

                case "invalid-csrf" -> new MissingCsrfTokenException(
                        "Sensitive CSRF details"
                );

                case "access-denied" -> new AccessDeniedException(
                        "Sensitive authorization details"
                );

                default -> new IllegalArgumentException(
                        "Unknown test case: " + caseName
                );
            };
        }
    }
}
