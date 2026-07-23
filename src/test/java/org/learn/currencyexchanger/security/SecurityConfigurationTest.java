package org.learn.currencyexchanger.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.TestController.class)
@Import({
        SecurityConfiguration.class,
        SecurityConfigurationTest.TestController.class
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAnonymousRegistrationWithCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .with(csrf())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectRegistrationWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoint() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isUnauthorized());
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

    @RestController
    public static class TestController {

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