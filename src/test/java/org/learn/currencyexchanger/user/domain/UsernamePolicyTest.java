package org.learn.currencyexchanger.user.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernamePolicyTest {

    @Test
    void shouldNormalizeUsername() {
        String normalized = UsernamePolicy.normalize("  John.DOE    ");
        assertEquals("john.doe", normalized);
    }

    @Test
    void shouldApplyUnicodeNormalization() {
        String normalized = UsernamePolicy.normalize("ＡＤＭＩＮ");
        assertEquals("admin", normalized);
    }

    @Test
    void shouldRejectNullUsername() {
        assertThrows(InvalidUsernameException.class, () -> UsernamePolicy.normalize(null));
    }

    @Test
    void shouldRejectTooShortUsername() {
        assertThrows(InvalidUsernameException.class, () -> UsernamePolicy.normalize("ab"));
    }

    @Test
    void shouldRejectUnsupportedCharacters() {
        assertThrows(InvalidUsernameException.class, () -> UsernamePolicy.normalize("john@example.com"));
    }
}