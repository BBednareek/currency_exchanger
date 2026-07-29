package org.learn.currencyexchanger.security.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.security.api.SecurityExceptionResolverBridge;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.learn.currencyexchanger.user.domain.User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActiveAccountSessionFilterTest {

    private static final String PASSWORD_HASH =
            "{bcrypt}password-hash";

    private UserRepository userRepository;
    private LogoutHandler logoutHandler;
    private SecurityExceptionResolverBridge exceptionHandler;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ActiveAccountSessionFilter filter;

    private static void setAuthentication(
            Authentication authentication
    ) {
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        logoutHandler = mock(LogoutHandler.class);
        exceptionHandler = mock(
                SecurityExceptionResolverBridge.class
        );
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest(
                "GET",
                "/api/users/me"
        );
        response = new MockHttpServletResponse();
        filter = new ActiveAccountSessionFilter(
                userRepository,
                logoutHandler,
                exceptionHandler
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueAnonymousRequest() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(
                userRepository,
                logoutHandler,
                exceptionHandler
        );
    }

    @Test
    void shouldIgnoreAuthenticationWithDifferentPrincipalType()
            throws Exception {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "john.doe",
                        null,
                        List.of()
                );
        setAuthentication(authentication);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(
                userRepository,
                logoutHandler,
                exceptionHandler
        );
    }

    @Test
    void shouldContinueRequestForActiveAccount()
            throws Exception {
        User user = authenticatedUser();

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        filter.doFilter(request, response, filterChain);

        verify(userRepository).findById(user.getId());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(
                logoutHandler,
                exceptionHandler
        );
    }

    @Test
    void shouldInvalidateSessionForLockedAccount()
            throws Exception {
        User user = authenticatedUser();
        user.lock();

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertSessionRejected();
    }

    @Test
    void shouldInvalidateSessionForDisabledAccount()
            throws Exception {
        User user = authenticatedUser();
        user.disable(
                Instant.parse("2026-07-27T10:15:30Z")
        );

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertSessionRejected();
    }

    @Test
    void shouldInvalidateSessionWhenAccountNoLongerExists()
            throws Exception {
        User user = authenticatedUser();

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.empty());

        assertSessionRejected();
    }

    private User authenticatedUser() {
        User user = User.register(
                "john.doe",
                PASSWORD_HASH
        );
        AppUserPrincipal principal =
                AppUserPrincipal.from(user);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        setAuthentication(authentication);

        return user;
    }

    private void assertSessionRejected() throws Exception {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        filter.doFilter(request, response, filterChain);

        verify(logoutHandler).logout(
                same(request),
                same(response),
                same(authentication)
        );

        var exceptionCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        AuthenticationException.class
                );

        verify(exceptionHandler).commence(
                same(request),
                same(response),
                exceptionCaptor.capture()
        );

        assertInstanceOf(
                InsufficientAuthenticationException.class,
                exceptionCaptor.getValue()
        );
        assertTrue(
                exceptionCaptor.getValue()
                        .getMessage()
                        .contains("no longer valid")
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }
}
