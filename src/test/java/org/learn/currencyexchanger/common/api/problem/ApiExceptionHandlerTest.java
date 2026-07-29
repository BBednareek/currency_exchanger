package org.learn.currencyexchanger.common.api.problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                        "unexpected",
                        ApiProblemCode.INTERNAL_ERROR,
                        ApiProblemCode.INTERNAL_ERROR.defaultDetail()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("mappedProblems")
    void shouldMapApplicatiFonDomainAndSecurityExceptions(
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
                case "data-conflict" -> new DataIntegrityViolationException(
                        "Sensitive database details"
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
