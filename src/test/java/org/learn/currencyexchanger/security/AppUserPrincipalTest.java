package org.learn.currencyexchanger.security;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.domain.User;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppUserPrincipalTest {
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Test
    void shouldEraseCredentials() {
        User user = User.register("john.doe", PASSWORD_HASH);
        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertNotNull(principal.getPassword());

        principal.eraseCredentials();

        assertNull(principal.getPassword());
    }
}