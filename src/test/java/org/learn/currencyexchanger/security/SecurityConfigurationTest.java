package org.learn.currencyexchanger.security;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.problem.ApiExceptionHandler;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.security.api.ApiSecurityExceptionHandler;
import org.learn.currencyexchanger.security.configuration.SecurityConfiguration;
import org.learn.currencyexchanger.security.configuration.SessionAuthenticationConfiguration;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.TestController.class)
@Import({
        SecurityConfiguration.class,
        SessionAuthenticationConfiguration.class,
        ApiSecurityExceptionHandler.class,
        ApiExceptionHandler.class,
        SecurityConfigurationTest.TestController.class,
        ApiProblemFactory.class
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LogoutHandler logoutHandler;

    @Test
    void shouldAllowAnonymousRegistrationWithCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .with(csrf())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldAllowAuthenticatedUserToAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .with(user("john.doe"))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldAllowAnonymousLoginWithCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForLogout() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedLogoutWithCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(user("john.doe"))
                                .with(csrf())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoint() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldRejectRegistrationWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CSRF_TOKEN"));
    }

    @Test
    void shouldRejectLogoutWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(user("john.doe"))
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CSRF_TOKEN"));
    }

    @RestController
    public static class TestController {

        @PostMapping("/api/auth/logout")
        ResponseEntity<Void> logout() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/api/auth/login")
        ResponseEntity<Void> login() {
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/api/auth/register")
        ResponseEntity<Void> register() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/api/users/me")
        ResponseEntity<Void> currentUser() {
            return ResponseEntity.noContent().build();
        }
    }
}
