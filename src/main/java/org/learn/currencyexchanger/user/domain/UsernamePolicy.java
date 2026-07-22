package org.learn.currencyexchanger.user.domain;

import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UsernamePolicy {
    private static final Pattern ALLOWED = Pattern.compile("[a-z0-9._-]{3,50}");

    // 3-50 znakow dla username
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    private UsernamePolicy(){}

    public static String normalize(String rawUsername) {
        if (rawUsername == null)
            throw new InvalidUsernameException("Username cannot be null");

        String normalizedUsername = Normalizer
                .normalize(rawUsername, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED.matcher(normalizedUsername).matches())
            throw new InvalidUsernameException("Username must containt 3-50 ASCII letters, digits, dots, " +
                    "underscores or hyphens");

        return normalizedUsername;
    }
}
