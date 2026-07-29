package org.learn.currencyexchanger.security.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.security.api.SecurityExceptionResolverBridge;
import org.learn.currencyexchanger.user.application.port.AccountStatusReader;
import org.learn.currencyexchanger.user.domain.User;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActiveAccountSessionFilterTest {

    private static final String PASSWORD_HASH =
            "{bcrypt}password-hash";

    private AccountStatusReader accountStatusReader;
    private LogoutHandler logoutHandler;
    private SecurityExceptionResolverBridge exceptionResolverBridge;
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
        accountStatusReader = mock(AccountStatusReader.class);
        logoutHandler = mock(LogoutHandler.class);
        exceptionResolverBridge = mock(
                SecurityExceptionResolverBridge.class
        );
        filterChain = mock(FilterChain.class);

        request = new MockHttpServletRequest(
                "GET",
                "/api/users/me"
        );
        response = new MockHttpServletResponse();

        filter = new ActiveAccountSessionFilter(
                accountStatusReader,
                logoutHandler,
                exceptionResolverBridge
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
                accountStatusReader,
                logoutHandler,
                exceptionResolverBridge
        );
    }

    @Test
    void shouldIgnoreAuthenticationWithUnsupportedPrincipal()
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
                accountStatusReader,
                logoutHandler,
                exceptionResolverBridge
        );
    }

    @Test
    void shouldContinueRequestForActiveAccount()
            throws Exception {
        UUID userId = authenticateUser();

        when(accountStatusReader.isActive(userId))
                .thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(accountStatusReader).isActive(userId);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(
                logoutHandler,
                exceptionResolverBridge
        );
    }

    @Test
    void shouldInvalidateSessionForInactiveAccount()
            throws Exception {
        UUID userId = authenticateUser();

        when(accountStatusReader.isActive(userId))
                .thenReturn(false);

        assertSessionRejected(userId);
    }

    private UUID authenticateUser() {
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

        return user.getId();
    }

    private void assertSessionRejected(UUID userId)
            throws Exception {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        filter.doFilter(request, response, filterChain);

        verify(accountStatusReader).isActive(userId);

        verify(logoutHandler).logout(
                same(request),
                same(response),
                same(authentication)
        );

        ArgumentCaptor<AuthenticationException> exceptionCaptor =
                ArgumentCaptor.forClass(
                        AuthenticationException.class
                );

        verify(exceptionResolverBridge).commence(
                same(request),
                same(response),
                exceptionCaptor.capture()
        );

        AuthenticationException exception =
                exceptionCaptor.getValue();

        assertInstanceOf(
                InsufficientAuthenticationException.class,
                exception
        );
        assertEquals(
                "Session is no longer valid",
                exception.getMessage()
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }
}
