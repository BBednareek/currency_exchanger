package org.learn.currencyexchanger.auth.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestTest {

    @Test
    void shouldRedactPasswordFromStringRepresentation() {
        String rawPassword = "correct horse battery staple";
        LoginRequest request =
                new LoginRequest("john.doe", rawPassword);

        String representation = request.toString();

        assertFalse(representation.contains(rawPassword));
        assertTrue(representation.contains("password=<redacted>"));
    }
}