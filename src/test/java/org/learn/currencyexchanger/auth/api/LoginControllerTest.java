package org.learn.currencyexchanger.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.ApiExceptionHandler;
import org.learn.currencyexchanger.security.AppUserPrincipal;
import org.learn.currencyexchanger.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class LoginControllerTest {

    private static final String USERNAME = "john.doe";
    private static final String RAW_PASSWORD =
            "correct horse battery staple";
    private static final String PASSWORD_HASH =
            "{bcrypt}password-hash";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @MockitoBean
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    private Authentication authenticationResult;

    @BeforeEach
    void setUp() {
        User user = User.register(USERNAME, PASSWORD_HASH);
        AppUserPrincipal principal = AppUserPrincipal.from(user);

        principal.eraseCredentials();

        authenticationResult =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAndSaveSecurityContext() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticationResult);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "john.doe",
                                          "password": "correct horse battery staple"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        var authenticationRequestCaptor =
                org.mockito.ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager)
                .authenticate(authenticationRequestCaptor.capture());

        Authentication authenticationRequest =
                authenticationRequestCaptor.getValue();

        assertAll(
                () -> assertFalse(authenticationRequest.isAuthenticated()),
                () -> assertEquals(
                        USERNAME,
                        authenticationRequest.getPrincipal()
                ),
                () -> assertEquals(
                        RAW_PASSWORD,
                        authenticationRequest.getCredentials()
                )
        );

        verify(sessionAuthenticationStrategy).onAuthentication(
                same(authenticationResult),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );

        var securityContextCaptor =
                org.mockito.ArgumentCaptor.forClass(SecurityContext.class);

        verify(securityContextRepository).saveContext(
                securityContextCaptor.capture(),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );

        assertSame(
                authenticationResult,
                securityContextCaptor.getValue().getAuthentication()
        );

        assertSame(
                authenticationResult,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": "john.doe",
                                          "password": "wrong password"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.title")
                        .value("Authentication failed"))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid username or password"));

        verifyNoInteractions(
                sessionAuthenticationStrategy,
                securityContextRepository
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @Test
    void shouldRejectBlankCredentialsBeforeAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                                .content("""
                                        {
                                          "username": " ",
                                          "password": " "
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.length()").value(2));

        verifyNoInteractions(
                authenticationManager,
                sessionAuthenticationStrategy,
                securityContextRepository
        );
    }
}