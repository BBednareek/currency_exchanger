package org.learn.currencyexchanger.user.api.problem;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.user.application.exception.ConcurrentUserModificationException;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                UserApiExceptionHandlerTest.TestController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        UserApiExceptionHandler.class,
        UserApiExceptionHandlerTest.TestController.class,
        ApiProblemFactory.class
})
class UserApiExceptionHandlerTest {

    private static final String BASE_PATH =
            "/api/test/problems/user";

    private static final UUID USER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001"
    );

    private final MockMvc mockMvc;

    @Autowired
    UserApiExceptionHandlerTest(MockMvc mockMvc) {
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
                        ApiProblemCode.USERNAME_ALREADY_USED
                                .defaultDetail()
                ),
                Arguments.of(
                        "invalid-username",
                        ApiProblemCode.INVALID_USERNAME,
                        "Username contains unsupported characters"
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
                        "concurrent-modification",
                        ApiProblemCode.CONCURRENT_MODIFICATION,
                        ApiProblemCode.CONCURRENT_MODIFICATION
                                .defaultDetail()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappedProblems")
    void shouldMapUserExceptionToProblemDetail(
            String caseName,
            ApiProblemCode expectedCode,
            String expectedDetail
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
                        expectedDetail
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
                case "user-not-found" -> new UserNotFoundException(USER_ID);

                case "username-already-used" -> new UsernameAlreadyUsedException();

                case "invalid-username" -> new InvalidUsernameException(
                        "Username contains unsupported characters"
                );

                case "disabled-user" -> new DisabledUserCannotBeModifiedException();

                case "cannot-unlock" -> new UserCannotBeUnlockedException(
                        UserStatus.ACTIVE
                );

                case "concurrent-modification" -> new ConcurrentUserModificationException(
                        USER_ID,
                        new IllegalStateException(
                                "Sensitive persistence details"
                        )
                );

                default -> new IllegalArgumentException(
                        "Unknown test case: " + caseName
                );
            };
        }
    }
}
