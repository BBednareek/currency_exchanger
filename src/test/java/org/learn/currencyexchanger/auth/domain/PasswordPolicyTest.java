package org.learn.currencyexchanger.auth.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void shouldAcceptValidPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("correct horse battery staple"));
    }

    @Test
    void shouldAcceptPasswordAtMinimumLength() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH)));
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate(null)
        );
    }

    @Test
    void shouldRejectBlankPassword() {
        assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate(" ".repeat(PasswordPolicy.MIN_LENGTH))
        );
    }

    @Test
    void shouldRejectPasswordShorterThanMinimumLength() {
        String password = "a".repeat(PasswordPolicy.MIN_LENGTH - 1);

        assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate(password)
        );
    }

    @Test
    void shouldAcceptPasswordAtBcryptByteLimit() {
        String password = "a".repeat(PasswordPolicy.MAX_BCRYPT_BYTES);

        assertDoesNotThrow(
                () -> PasswordPolicy.validate(password)
        );
    }

    @Test
    void shouldRejectPasswordExceedingBcryptByteLimit() {
        String password = "a".repeat(PasswordPolicy.MAX_BCRYPT_BYTES + 1);

        assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate(password)
        );
    }

    @Test
    void shouldCalculateBcryptLimitUsingUtf8Bytes() {
        String password = "ą".repeat(37); //37 znakow, ale 74 bajty utf8

        assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate(password)
        );

    }
}