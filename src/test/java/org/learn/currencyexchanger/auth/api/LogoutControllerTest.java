package org.learn.currencyexchanger.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.common.api.problem.FrameworkApiExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogoutController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        ApiProblemFactory.class,
        FrameworkApiExceptionHandler.class
})
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogoutHandler logoutHandler;

    @Test
    void shouldLogoutCurrentUser() throws Exception {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "john.doe",
                        null,
                        List.of()
                );

        mockMvc.perform(
                        post("/api/auth/logout")
                                .principal(authentication)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(logoutHandler).logout(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                same(authentication)
        );
    }
}
