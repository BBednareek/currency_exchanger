package org.learn.currencyexchanger.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
        @NotBlank
        @Size(max = 320)
        String username
) {
}
