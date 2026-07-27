package org.learn.currencyexchanger.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.security.auth.AppUserPrincipal;
import org.learn.currencyexchanger.security.auth.DatabaseUserDetailsService;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.learn.currencyexchanger.user.domain.User;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    private static final String USERNAME = "john.doe";
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByNormalizedUsername() {
        User user = User.register(USERNAME, PASSWORD_HASH);

        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(user));

        AppUserPrincipal principal = assertInstanceOf(
                AppUserPrincipal.class,
                userDetailsService.loadUserByUsername("  John.DOE  ")
        );

        assertAll(
                () -> assertEquals(user.getId(), principal.getUserId()),
                () -> assertEquals(USERNAME, principal.getUsername()),
                () -> assertEquals(PASSWORD_HASH, principal.getPassword()),
                () -> assertTrue(principal.isEnabled())
        );

        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void shouldRejectUnknownUsernameWithGenericMessage() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(USERNAME)
        );

        assertEquals(INVALID_CREDENTIALS, exception.getMessage());

        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void shouldRejectInvalidUsernameWithoutCallingRepository() {
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("ab")
        );

        assertEquals(INVALID_CREDENTIALS, exception.getMessage());

        verifyNoInteractions(userRepository);
    }
}