package org.learn.currencyexchanger.user.domain;

import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UsernamePolicy {

    // 3-50 znakow dla username
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    private static final Pattern ALLOWED_USERNAME_PATTERN = Pattern.compile("[a-z0-9._-]{" + MIN_LENGTH + "," + MAX_LENGTH + "}");

    private UsernamePolicy() {
    }

    public static String normalize(String rawUsername) {
        if (rawUsername == null)
            throw new InvalidUsernameException("Username cannot be null");

        String normalizedUsername = Normalizer
                .normalize(rawUsername, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_USERNAME_PATTERN.matcher(normalizedUsername).matches())
            throw new InvalidUsernameException("Username must contain 3-50 ASCII letters, digits, dots, " +
                    "underscores or hyphens");

        return normalizedUsername;
    }
}
