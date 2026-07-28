package org.learn.currencyexchanger.auth;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.auth.api.LoginRequest;
import org.learn.currencyexchanger.auth.api.RegisterRequest;
import org.learn.currencyexchanger.security.api.CsrfTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD =
            "correct horse battery staple";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldCompleteSessionAuthenticationLifecycle() throws Exception {
        String username = "flow_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        CsrfSession anonymousCsrf = obtainCsrfToken(null);
        String anonymousSessionId =
                anonymousCsrf.session().getId();

        mockMvc.perform(
                        post("/api/auth/register")
                                .session(anonymousCsrf.session())
                                .header(
                                        anonymousCsrf.headerName(),
                                        anonymousCsrf.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsBytes(
                                        new RegisterRequest(
                                                username,
                                                PASSWORD
                                        )
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .session(anonymousCsrf.session())
                                .header(
                                        anonymousCsrf.headerName(),
                                        anonymousCsrf.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsBytes(
                                        new LoginRequest(
                                                username,
                                                PASSWORD
                                        )
                                ))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult
                        .getRequest()
                        .getSession(false);

        assertNotNull(authenticatedSession);
        assertNotEquals(
                anonymousSessionId,
                authenticatedSession.getId()
        );

        mockMvc.perform(
                        get("/api/users/me")
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Token używany przed zalogowaniem musi przestać być ważny.
        mockMvc.perform(
                        post("/api/auth/logout")
                                .session(authenticatedSession)
                                .header(
                                        anonymousCsrf.headerName(),
                                        anonymousCsrf.token()
                                )
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CSRF_TOKEN"));

        CsrfSession authenticatedCsrf =
                obtainCsrfToken(authenticatedSession);

        mockMvc.perform(
                        post("/api/auth/logout")
                                .session(authenticatedSession)
                                .header(
                                        authenticatedCsrf.headerName(),
                                        authenticatedCsrf.token()
                                )
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(authenticatedSession.isInvalid());

        mockMvc.perform(
                        get("/api/users/me")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldInvalidateOtherSessionAfterAccountIsDisabled()
            throws Exception {
        String username = "multi_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        CsrfSession firstAnonymousSession =
                obtainCsrfToken(null);

        registerUser(
                username,
                firstAnonymousSession
        );

        MockHttpSession firstAuthenticatedSession =
                login(
                        username,
                        firstAnonymousSession
                );

        CsrfSession secondAnonymousSession =
                obtainCsrfToken(null);
        MockHttpSession secondAuthenticatedSession =
                login(
                        username,
                        secondAnonymousSession
                );

        CsrfSession firstAuthenticatedCsrf =
                obtainCsrfToken(
                        firstAuthenticatedSession
                );

        mockMvc.perform(
                        delete("/api/users/me")
                                .session(
                                        firstAuthenticatedSession
                                )
                                .header(
                                        firstAuthenticatedCsrf
                                                .headerName(),
                                        firstAuthenticatedCsrf
                                                .token()
                                )
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(firstAuthenticatedSession.isInvalid());

        mockMvc.perform(
                        get("/api/users/me")
                                .session(
                                        secondAuthenticatedSession
                                )
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/users/me"));

        assertTrue(secondAuthenticatedSession.isInvalid());
    }

    private void registerUser(
            String username,
            CsrfSession csrf
    ) throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .session(csrf.session())
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsBytes(
                                        new RegisterRequest(
                                                username,
                                                PASSWORD
                                        )
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username")
                        .value(username));
    }

    private MockHttpSession login(
            String username,
            CsrfSession csrf
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .session(csrf.session())
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsBytes(
                                        new LoginRequest(
                                                username,
                                                PASSWORD
                                        )
                                ))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false);

        assertNotNull(session);

        return session;
    }

    private CsrfSession obtainCsrfToken(
            MockHttpSession existingSession
    ) throws Exception {
        MockHttpServletRequestBuilder request =
                get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON);

        if (existingSession != null) {
            request.session(existingSession);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andReturn();

        CsrfTokenResponse csrf = jsonMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                CsrfTokenResponse.class
        );

        MockHttpSession session = (MockHttpSession) result
                .getRequest()
                .getSession(false);

        assertNotNull(session);

        return new CsrfSession(
                session,
                csrf.headerName(),
                csrf.token()
        );
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}
