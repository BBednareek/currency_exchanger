package org.learn.currencyexchanger.auth.api.problem;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                AuthApiExceptionHandlerTest.TestController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        AuthApiExceptionHandler.class,
        ApiProblemFactory.class,
        AuthApiExceptionHandlerTest.TestController.class
})
class AuthApiExceptionHandlerTest {

    private static final String PATH =
            "/api/test/problems/auth/invalid-password";

    private static final String EXCEPTION_MESSAGE =
            "Password must contain at least 12 characters";

    private final MockMvc mockMvc;

    @Autowired
    AuthApiExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldMapInvalidPasswordToProblemDetail()
            throws Exception {
        ApiProblemCode expectedCode =
                ApiProblemCode.INVALID_PASSWORD;

        mockMvc.perform(
                        get(PATH)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
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
                        EXCEPTION_MESSAGE
                ))
                .andExpect(jsonPath("$.instance").value(PATH))
                .andExpect(jsonPath("$.code").value(
                        expectedCode.name()
                ))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @RestController
    @RequestMapping("/api/test/problems/auth")
    public static class TestController {

        @GetMapping("/invalid-password")
        void throwInvalidPasswordException() {
            throw new InvalidPasswordException(
                    EXCEPTION_MESSAGE
            );
        }
    }
}
