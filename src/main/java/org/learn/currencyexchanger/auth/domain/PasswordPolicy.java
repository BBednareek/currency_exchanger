package org.learn.currencyexchanger.auth.domain;

import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_BCRYPT_BYTES = 72;


    private PasswordPolicy() {

    }

    public static void validate(String rawPassword) {
        if (rawPassword == null) {
            throw new InvalidPasswordException("Password cannot be null");
        }

        if (rawPassword.isBlank()) {
            throw new InvalidPasswordException("Password cannot be blank");
        }

        int characterCount = rawPassword.codePointCount(0, rawPassword.length());

        if (characterCount < MIN_LENGTH) {
            throw new InvalidPasswordException(
                    "Password must contain at least " + MIN_LENGTH + " characters"
            );
        }

        int utf8Length = rawPassword.getBytes(StandardCharsets.UTF_8).length;

        if (utf8Length > MAX_BCRYPT_BYTES) {
            throw new InvalidPasswordException(
                    "Password cannot exceed " + MAX_BCRYPT_BYTES + " bytes"
            );
        }
    }
}
