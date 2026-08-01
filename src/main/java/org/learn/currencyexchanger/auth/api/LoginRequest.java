package org.learn.currencyexchanger.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.learn.currencyexchanger.auth.domain.PasswordPolicy;

public record LoginRequest(
        @Size(max = 100)
        @NotBlank(message = "Username is required")
        String username,

        @Size(
                max = PasswordPolicy.MAX_BCRYPT_BYTES,
                message = "Password cannot exceed 72 characters"
        )
        @NotBlank(message = "Password is required")
        String password

) {

    @Override
    public String toString() {
        return "LoginRequest[username=%s, password=<redacted>]".formatted(username);
    }
}
