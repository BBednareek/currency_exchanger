package org.learn.currencyexchanger.auth;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.auth.api.LoginRequest;
import org.learn.currencyexchanger.auth.api.RegisterRequest;
import org.learn.currencyexchanger.auth.application.RegistrationService;
import org.learn.currencyexchanger.security.api.CsrfTokenResponse;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.learn.currencyexchanger.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthenticationIntegrationTest {

    private static final String PASSWORD =
            "correct horse battery staple";

    private static final String WRONG_PASSWORD =
            "definitely wrong password";

    private static final Instant DISABLED_AT =
            Instant.parse("2026-07-27T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegistrationService registrationService;

    private static String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    @Test
    void shouldLoginUsingNormalizedUsername()
            throws Exception {
        String normalizedUsername =
                uniqueUsername("normalized_");

        String rawUsername = "  "
                + normalizedUsername.toUpperCase(Locale.ROOT)
                + "  ";

        CsrfSession csrf = obtainCsrfToken();

        mockMvc.perform(
                        post("/api/auth/register")
                                .session(csrf.session())
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        jsonMapper.writeValueAsBytes(
                                                new RegisterRequest(
                                                        rawUsername,
                                                        PASSWORD
                                                )
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username")
                        .value(normalizedUsername));

        login(
                csrf,
                rawUsername,
                PASSWORD
        )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldRejectIncorrectPassword()
            throws Exception {
        User user = persistUser(
                "wrong_password_"
        );

        assertAuthenticationRejected(
                user.getUsername(),
                WRONG_PASSWORD
        );
    }

    @Test
    void shouldRejectLockedUser()
            throws Exception {
        User user = persistUser(
                "locked_",
                User::lock
        );

        assertAuthenticationRejected(
                user.getUsername(),
                PASSWORD
        );
    }

    @Test
    void shouldRejectDisabledUser()
            throws Exception {
        User user = persistUser(
                "disabled_",
                account -> account.disable(DISABLED_AT)
        );

        assertAuthenticationRejected(
                user.getUsername(),
                PASSWORD
        );
    }

    @Test
    void shouldAllowOnlyOneConcurrentRegistration()
            throws Exception {
        String username = uniqueUsername("race_");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            Future<RegistrationOutcome> first =
                    executor.submit(() -> registerConcurrently(
                            username,
                            ready,
                            start
                    ));

            Future<RegistrationOutcome> second =
                    executor.submit(() -> registerConcurrently(
                            username,
                            ready,
                            start
                    ));

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Registration tasks did not become ready"
            );

            start.countDown();

            List<RegistrationOutcome> outcomes = List.of(
                    first.get(),
                    second.get()
            );

            assertEquals(
                    1,
                    Collections.frequency(
                            outcomes,
                            RegistrationOutcome.CREATED
                    )
            );

            assertEquals(
                    1,
                    Collections.frequency(
                            outcomes,
                            RegistrationOutcome.USERNAME_ALREADY_USED
                    )
            );
        }
    }

    private RegistrationOutcome registerConcurrently(
            String username,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();

        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Registration start signal was not received"
            );
        }

        try {
            registrationService.register(
                    username,
                    PASSWORD
            );

            return RegistrationOutcome.CREATED;
        } catch (UsernameAlreadyUsedException exception) {
            return RegistrationOutcome.USERNAME_ALREADY_USED;
        }
    }

    private User persistUser(String prefix) {
        return persistUser(
                prefix,
                user -> {
                }
        );
    }

    private User persistUser(
            String prefix,
            Consumer<User> stateChange
    ) {
        User user = User.register(
                uniqueUsername(prefix),
                passwordEncoder.encode(PASSWORD)
        );

        stateChange.accept(user);

        return userRepository.save(user);
    }

    private void assertAuthenticationRejected(
            String username,
            String password
    ) throws Exception {
        CsrfSession csrf = obtainCsrfToken();

        login(
                csrf,
                username,
                password
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid username or password"));
    }

    private ResultActions login(
            CsrfSession csrf,
            String username,
            String password
    ) throws Exception {
        return mockMvc.perform(
                post("/api/auth/login")
                        .session(csrf.session())
                        .header(
                                csrf.headerName(),
                                csrf.token()
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .accept(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                        .content(
                                jsonMapper.writeValueAsBytes(
                                        new LoginRequest(
                                                username,
                                                password
                                        )
                                )
                        )
        );
    }

    private CsrfSession obtainCsrfToken()
            throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/auth/csrf")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        CsrfTokenResponse csrfToken =
                jsonMapper.readValue(
                        result.getResponse()
                                .getContentAsByteArray(),
                        CsrfTokenResponse.class
                );

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false);

        assertNotNull(session);

        return new CsrfSession(
                session,
                csrfToken.headerName(),
                csrfToken.token()
        );
    }

    private enum RegistrationOutcome {
        CREATED,
        USERNAME_ALREADY_USED
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token
    ) {
    }
}
