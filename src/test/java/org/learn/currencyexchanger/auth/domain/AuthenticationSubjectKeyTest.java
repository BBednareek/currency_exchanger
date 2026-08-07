package org.learn.currencyexchanger.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationSubjectKeyTest {

    @Test
    void shouldCreateTheSameKeyForEquivalentUsernames() {
        AuthenticationSubjectKey canonical =
                AuthenticationSubjectKey.fromUsername("john.doe");

        AuthenticationSubjectKey nonCanonical =
                AuthenticationSubjectKey.fromUsername("  JOHN.DOE  ");

        assertEquals(canonical, nonCanonical);
    }

    @Test
    void shouldNotExposeUsernameInTheKey() {
        AuthenticationSubjectKey key =
                AuthenticationSubjectKey.fromUsername("john.doe");

        assertEquals(64, key.value().length());
        assertFalse(key.value().contains("john.doe"));
    }

    @Test
    void shouldRejectMalformedKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationSubjectKey("not-a-sha-256-key")
        );
    }
}
