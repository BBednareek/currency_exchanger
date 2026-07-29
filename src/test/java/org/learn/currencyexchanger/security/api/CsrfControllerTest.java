package org.learn.currencyexchanger.security.api;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.common.api.problem.FrameworkApiExceptionHandler;
import org.learn.currencyexchanger.security.api.problem.SecurityApiExceptionHandler;
import org.learn.currencyexchanger.security.configuration.SecurityConfiguration;
import org.learn.currencyexchanger.security.configuration.SessionAuthenticationConfiguration;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CsrfController.class)
@Import({
        SecurityConfiguration.class,
        SessionAuthenticationConfiguration.class,
        SecurityExceptionResolverBridge.class,
        SecurityApiExceptionHandler.class,
        FrameworkApiExceptionHandler.class,
        ApiProblemFactory.class
})
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LogoutHandler logoutHandler;

    @Test
    void shouldReturnCsrfTokenForAnonymousClient() throws Exception {
        mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
