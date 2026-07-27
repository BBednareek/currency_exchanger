package org.learn.currencyexchanger.security;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.security.auth.AppUserPrincipal;
import org.learn.currencyexchanger.user.domain.User;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserPrincipalTest {

    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Test
    void shouldCreatePrincipalForActiveUser() {
        User user = User.register("john.doe", PASSWORD_HASH);

        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertAll(
                () -> assertEquals(user.getId(), principal.getUserId()),
                () -> assertEquals("john.doe", principal.getUsername()),
                () -> assertEquals(PASSWORD_HASH, principal.getPassword()),
                () -> assertEquals(
                        "ROLE_USER",
                        principal.getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority()
                ),
                () -> assertTrue(principal.isAccountNonLocked()),
                () -> assertTrue(principal.isEnabled()),
                () -> assertTrue(principal.isAccountNonExpired()),
                () -> assertTrue(principal.isCredentialsNonExpired())
        );
    }

    @Test
    void shouldRepresentLockedUserAsLockedButEnabled() {
        User user = User.register("john.doe", PASSWORD_HASH);
        user.lock();

        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertAll(
                () -> assertFalse(principal.isAccountNonLocked()),
                () -> assertTrue(principal.isEnabled())
        );
    }

    @Test
    void shouldRepresentDisabledUserAsDisabled() {
        User user = User.register("john.doe", PASSWORD_HASH);
        user.disable(Instant.parse("2026-07-27T10:15:30Z"));

        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertAll(
                () -> assertTrue(principal.isAccountNonLocked()),
                () -> assertFalse(principal.isEnabled())
        );
    }

    @Test
    void shouldEraseCredentials() {
        User user = User.register("john.doe", PASSWORD_HASH);
        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertNotNull(principal.getPassword());

        principal.eraseCredentials();

        assertNull(principal.getPassword());
    }
}
