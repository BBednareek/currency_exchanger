package org.learn.currencyexchanger.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public record AuthenticationSubjectKey(String value) {

    private static final int SHA_256_HEX_LENGTH = 64;

    public AuthenticationSubjectKey {
        Objects.requireNonNull(value, "Authentication subject key must not be null");

        if (!isLowercaseSha256(value)) {
            throw new IllegalArgumentException(
                    "Authentication subject key must be a lowercase SHA-256 value"
            );
        }
    }

    public static AuthenticationSubjectKey fromUsername(String rawUsername) {
        Objects.requireNonNull(rawUsername, "Username must not be null");

        String canonicalUsername = Normalizer
                .normalize(rawUsername, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);

        byte[] digest = sha256().digest(
                canonicalUsername.getBytes(StandardCharsets.UTF_8)
        );

        return new AuthenticationSubjectKey(
                HexFormat.of().formatHex(digest)
        );
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "The JVM does not provide the required SHA-256 algorithm",
                    exception
            );
        }
    }

    private static boolean isLowercaseSha256(String candidate) {
        if (candidate.length() != SHA_256_HEX_LENGTH) {
            return false;
        }

        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);

            boolean isDigit = character >= '0' && character <= '9';
            boolean isLowercaseHex = character >= 'a' && character <= 'f';

            if (!isDigit && !isLowercaseHex) {
                return false;
            }
        }

        return true;
    }
}
