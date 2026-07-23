package org.learn.currencyexchanger.auth.api;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.auth.application.AuthService;
import org.learn.currencyexchanger.auth.application.RegistrationResult;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.common.api.ApiExceptionHandler;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AuthControllerTest {

    private static final String USERNAME = "john.doe";
    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldRegisterUser() throws Exception {
        UUID userId = UUID.randomUUID();

        when(authService.register(USERNAME, PASSWORD))
                .thenReturn(new RegistrationResult(
                        userId,
                        USERNAME,
                        UserRole.USER,
                        UserStatus.ACTIVE
                ));

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "john.doe",
                                          "password": "correct horse battery staple"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authService).register(USERNAME, PASSWORD);
    }

    @Test
    void shouldReturnValidationProblemForInvalidUsername() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": "ab",
                                          "password": "correct horse battery staple"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].path").value("username"))
                .andExpect(jsonPath("$.violations[0].message").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnValidationProblemWhenPasswordIsMissing() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": "john.doe"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].path").value("password"))
                .andExpect(jsonPath("$.violations[0].message").value("Password is required"));

        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnInvalidPasswordProblem() throws Exception {
        when(authService.register(USERNAME, "too-short"))
                .thenThrow(new InvalidPasswordException(
                        "Password must contain at least 12 characters"
                ));

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": "john.doe",
                                          "password": "too-short"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.title").value("Invalid password"))
                .andExpect(jsonPath("$.detail")
                        .value("Password must contain at least 12 characters"));

        verify(authService).register(USERNAME, "too-short");
    }

    @Test
    void shouldReturnConflictWhenUsernameIsAlreadyUsed() throws Exception {
        when(authService.register(USERNAME, PASSWORD))
                .thenThrow(new UsernameAlreadyUsedException());

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": "john.doe",
                                          "password": "correct horse battery staple"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_USED"))
                .andExpect(jsonPath("$.title").value("Username already used"));

        verify(authService).register(USERNAME, PASSWORD);
    }
}